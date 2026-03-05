package org.ddh.gamsapi.application.Integration.ApiSearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationDataProcessingException;
import org.ddh.gamsapi.application.Integration.Common.interfaces.IIntegrationService;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrClient;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrDocument;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrGamsCores;
import org.ddh.gamsapi.domain.Datastream.DatastreamId;
import org.ddh.gamsapi.domain.Datastream.utils.GAMSDsid;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamMimeView;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.ddh.gamsapi.domain.Datastream.utils.exceptions.DatastreamCannotLoadFileException;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.DublinCoreEntrySummaryView;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.application.Integration.Common.utils.XMLUtils;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.*;

/**
 * Service responsible for indexing digital objects into Apache Solr for the API search (g-search core).
 *
 * <p>This service creates Solr documents from digital objects including their Dublin Core metadata,
 * fulltext content, and administrative metadata. It handles multi-language Dublin Core values
 * using a hybrid indexing strategy.</p>
 *
 * <h3>Multi-Language Indexing Strategy</h3>
 * <p>Dublin Core entries are indexed into three types of Solr fields:</p>
 * <ul>
 *   <li><b>Combined search fields</b> ({@code dc.title}): All language values combined (clean, no language prefix)
 *       for cross-language fulltext search and faceting. Duplicate values across languages are removed.</li>
 *   <li><b>Tokenized fulltext fields</b> ({@code dc.title_txt}): Automatically populated via Solr copyField rules
 *       for substring matching.</li>
 *   <li><b>Language-specific display fields</b> ({@code dc.title.en}): Clean values per language,
 *       enabling clients to display appropriate language content without parsing.</li>
 * </ul>
 *
 * @see DublinCoreSolrFieldConfig
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApiSearchService implements IIntegrationService {

  private final SolrClient solrClient;
  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;
  private final IDublinCoreEntryRepository dublinCoreEntryRepository;
  private final IDatastreamContentRepository datastreamContentRepository;

  @Override
  public void indexObjects(String projectAbbr) {
    var digitalObjects = digitalObjectRepository.findAllByProject_ProjectAbbr(projectAbbr);
    digitalObjects.forEach(digitalObject -> indexObject(projectAbbr, digitalObject.getId()));
  }

  @Override
  public void deleteIndexedObjects(String projectAbbr) {
    log.trace("*** Trying to delete solr indexed project objects for: {}", projectAbbr);

    // delete selected from GAMS core
    solrClient.delete(SolrGamsCores.GAMS_CORE.value, String.format("%s:%s", ApiSearchProperties.PROJECT.name, projectAbbr));

  }

  @Override
  public void indexObject(String projectAbbr, String id) {

    log.trace("*** BaseSearchService: Indexing now object with id {} for project {}", id, projectAbbr);

    DigitalObject digitalObject = digitalObjectRepository.findById(id)
        .orElseThrow(() -> new IntegrationDataProcessingException(String.format("Digital object with id %s not found", id)));

    SolrDocument solrDocument = new SolrDocument();

    var foundDatastreams = datastreamRepository.findAllDatastreamMimeViewsByDigitalObject(digitalObject);

    // id needs to stay the same -- otherwise multiple entries with same ids will be created.
    solrDocument.addProperty(ApiSearchProperties.OBJECT_ID.name, digitalObject.getId());
    solrDocument.addProperty(ApiSearchProperties.PROJECT.name, digitalObject.getProject().getProjectAbbr());
    solrDocument.addProperty(ApiSearchProperties.TYPE.name, ApiSearchTypes.DIGITAL_OBJECT.name);
    // index datastream ids
    if(!foundDatastreams.isEmpty()){
      solrDocument.addProperty(ApiSearchProperties.DATASTREAMS.name, foundDatastreams.stream().map(IDatastreamMimeView::getDsid).toList());
    }

    // index tags if present
    if(!digitalObject.getTags().isEmpty()){
      solrDocument.addProperty(
          ApiSearchProperties.TAGS.name,
          digitalObject.getTags()
              .stream()
              .toList()
      );
    }

    // These fields might differ from the dublin core!
    solrDocument.addProperty(ApiSearchProperties.TITLE.name, digitalObject.getBaseMetadata().getTitle());
    solrDocument.addProperty(ApiSearchProperties.DESCRIPTION.name, digitalObject.getBaseMetadata().getDescription());
    solrDocument.addProperty(ApiSearchProperties.CREATOR.name, digitalObject.getBaseMetadata().getCreator());
    solrDocument.addProperty(ApiSearchProperties.PUBLISHER.name, digitalObject.getPublisher());
    solrDocument.addProperty(ApiSearchProperties.RIGHTS.name, digitalObject.getBaseMetadata().getRights());


    // send datastream contained info to solr
    // based on conditions formulated by the datastream's metadata e.g.
    // mimetype or dsid value, like DC.xml
    foundDatastreams.forEach(datastream -> {
      DatastreamId datastreamId =  DatastreamId.builder().dsid(datastream.getDsid()).digitalObject(id).build();
      // send custom search datastream directly to solr
      // TODO think about disabled custom solr indexing
//      if(datastream.getDsid().equals(GAMSAPIntegrationDatastreamId.SEARCH_DATASTREAM_ID.name)) {
//        sendCustomSolrDatastream(datastreamId, projectAbbr);
//      }

      if(datastream.getDsid().equals(GAMSDsid.DC.getValue())){
        addDublinCore(solrDocument, datastreamId);
      }

    });

    // TODO add logging
    // add fulltext only for main resource or DC.xml
    var fulltextDsid = digitalObject.getMainResource();
    if(fulltextDsid == null || fulltextDsid.isEmpty()) {
      fulltextDsid = GAMSDsid.DC.getValue();
    }

    // additionally check if datastream is xml
    // TODO this is not elegant - because now the file ending must be contained - is this correct?
    // TODO add logging
    if(!fulltextDsid.contains(".xml")){
      fulltextDsid = GAMSDsid.DC.getValue();
    }

    addFulltext(
        solrDocument,
        DatastreamId.builder().digitalObject(digitalObject.getId()).dsid(fulltextDsid).build()
    );



    // the end post base search entity to SOLR
    solrClient.post(SolrGamsCores.GAMS_CORE.value, solrDocument, true);
    log.info("Successfully created SOLR document representing digital object {}", digitalObject.getId());

  }


  @Override
  public void deleteIndexedObject(String projectAbbr, String id) {

    // escape colons in id (goes through the webclient and solr)
    id = id.replaceAll(":", "\\\\\\\\:");

    // delete object from GAMS core
    solrClient.delete(SolrGamsCores.GAMS_CORE.value, String.format("%s:%s", ApiSearchProperties.OBJECT_ID.name, id));
    // this requires solr documents to have the projectAbbr field
    solrClient.delete(projectAbbr, String.format("%s:%s", ApiSearchProperties.OBJECT_ID.name, id));

  }


  /**
   * Adds Dublin Core fields to the given Solr document using the hybrid multi-language strategy.
   *
   * <p><b>Indexing produces three field types per DC entry:</b></p>
   * <ol>
   *   <li><b>Combined search field</b> ({@code dc.title}): Clean value (no language prefix) containing
   *       all language variants. Used for fulltext search and faceting. Duplicate values across
   *       languages are automatically removed.</li>
   *   <li><b>Language-specific display field</b> ({@code dc.title.en}): Clean value for a specific
   *       language. Entries without a language tag use {@code dc.title.und}.
   *       These fields are stored but not indexed in Solr (display-only).</li>
   *   <li><b>Tokenized search field</b> ({@code dc.title_txt}): Automatically populated via
   *       Solr copyField rule from {@code dc.title}. Enables substring matching.</li>
   * </ol>
   *
   * <p><b>Example:</b> Given two DC entries for "title":</p>
   * <pre>
   *   DublinCoreEntry(name="title", value="Der Titel", language="de")
   *   DublinCoreEntry(name="title", value="The Title", language="en")
   * </pre>
   * <p>The Solr document will contain:</p>
   * <pre>
   *   dc.title          = ["Der Titel", "The Title"]     // search + facet
   *   dc.title.de  = ["Der Titel"]                  // display
   *   dc.title.en  = ["The Title"]                  // display
   *   dc.title_txt      = (auto via copyField)           // fulltext/substring
   * </pre>
   *
   * @param solrDocument Solr document to add Dublin Core fields to (modified in place)
   * @param datastreamId Datastream ID pointing to the DC.xml datastream
   * @throws IntegrationDataProcessingException if no Dublin Core entries found for the digital object
   */
  public void addDublinCore(SolrDocument solrDocument, DatastreamId datastreamId) {
    var dcEntries = dublinCoreEntryRepository.findByDigitalObjectId(datastreamId.getDigitalObject());
    if (dcEntries.isEmpty()) {
      String msg = String.format("No dublin core entries found for digital object %s", datastreamId.getDigitalObject());
      log.error(msg);
      throw new IntegrationDataProcessingException(msg);
    }

    // Phase 1: Group entries by DC field name, collecting values per field and per language.
    // This allows us to deduplicate values in the combined search field.
    //
    // Structure:  fieldName -> { "all" -> Set<String>, "en" -> Set<String>, "de" -> Set<String>, ... }
    //             where "all" collects every value for the combined search field (dc.title)
    //             and each language code collects values for the display field (dc.title.en)
    Map<String, Map<String, LinkedHashSet<String>>> fieldLanguageValues = new LinkedHashMap<>();

    for (DublinCoreEntrySummaryView dcEntry : dcEntries) {
      String dcFieldName = dcEntry.getName();
      String cleanValue = dcEntry.getValue();
      String language = dcEntry.getLanguage();

      // Initialize the field map if not present
      fieldLanguageValues.computeIfAbsent(dcFieldName, k -> new LinkedHashMap<>());
      Map<String, LinkedHashSet<String>> langMap = fieldLanguageValues.get(dcFieldName);

      // Add to combined search set (deduplicates across languages)
      langMap.computeIfAbsent("all", k -> new LinkedHashSet<>()).add(cleanValue);

      // Add to language-specific set
      String langCode = (language != null && !language.isBlank())
          ? language.trim().toLowerCase()
          : DublinCoreSolrFieldConfig.UNDEFINED_LANGUAGE_CODE;
      langMap.computeIfAbsent(langCode, k -> new LinkedHashSet<>()).add(cleanValue);
    }

    // Phase 2: Write grouped values into the Solr document
    for (Map.Entry<String, Map<String, LinkedHashSet<String>>> fieldEntry : fieldLanguageValues.entrySet()) {
      String dcFieldName = fieldEntry.getKey();
      Map<String, LinkedHashSet<String>> langMap = fieldEntry.getValue();

      // Write combined search field: dc.{name} — all values, deduplicated
      Set<String> allValues = langMap.get("all");
      if (allValues != null && !allValues.isEmpty()) {
        String searchFieldName = DublinCoreSolrFieldConfig.buildSearchFieldName(dcFieldName);
        solrDocument.addProperty(searchFieldName, new ArrayList<>(allValues));
      }

      // Write language-specific display fields: dc.{name}.{lang}
      for (Map.Entry<String, LinkedHashSet<String>> langEntry : langMap.entrySet()) {
        String langCode = langEntry.getKey();
        if ("all".equals(langCode)) {
          continue; // skip the "all" bucket — already written above
        }
        Set<String> langValues = langEntry.getValue();
        if (langValues != null && !langValues.isEmpty()) {
          String langFieldName = DublinCoreSolrFieldConfig.buildLanguageFieldName(dcFieldName, langCode);
          solrDocument.addProperty(langFieldName, new ArrayList<>(langValues));
        }
      }
    }

    log.debug("Added Dublin Core to Solr document for object {}: {} DC fields with language-specific display fields",
        datastreamId.getDigitalObject(), fieldLanguageValues.size());
  }


  /**
   * Adds fulltext field to given base search entity.
   * TODO test
   * @param solrDocument base search entity
   * @param datastreamId datastream id
   */
  public void addFulltext(SolrDocument solrDocument, DatastreamId datastreamId){

    var xmlContent =  datastreamContentRepository.findById(datastreamId);
    Document dcXml;
    try {
      dcXml = XMLUtils.parseXml(xmlContent.getInputStream());
    } catch (IOException e) {
      String msg = String.format("Failed to read datastream content %s for datastream %s. Original error: %s", xmlContent.getDescription(), datastreamId, e);
      log.error(msg);
      throw new DatastreamCannotLoadFileException(msg);
    } catch (IntegrationDataProcessingException e) {
      String msg = String.format("Failed to parse xml datastream %s. Original error: %s", datastreamId, e);
      log.error(msg);
      throw new DatastreamCannotLoadFileException(msg);
    }

    String docText = XMLUtils.extractText(dcXml);

    if(solrDocument.getProperty(ApiSearchProperties.FULLTEXT.name) == null){
      solrDocument.addProperty(ApiSearchProperties.FULLTEXT.name, docText);
    } else {
      String existingText = (String) solrDocument.getProperty(ApiSearchProperties.FULLTEXT.name);
      solrDocument.addProperty(ApiSearchProperties.FULLTEXT.name, existingText + "; " + docText  );
    }

  }

}