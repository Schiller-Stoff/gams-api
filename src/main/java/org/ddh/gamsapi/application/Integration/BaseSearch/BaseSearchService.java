package org.ddh.gamsapi.application.Integration.BaseSearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrClient;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrFacetedResponse;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrGamsCores;
import org.ddh.gamsapi.application.Integration.Common.enums.GAMSAPIntegrationDatastreamId;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationDataProcessingException;
import org.ddh.gamsapi.application.Integration.Common.interfaces.IIntegrationService;
import org.ddh.gamsapi.application.Integration.Common.utils.XMLUtils;
import org.ddh.gamsapi.domain.Datastream.DatastreamId;
import org.ddh.gamsapi.domain.Datastream.utils.GAMSDsid;
import org.ddh.gamsapi.domain.Datastream.utils.exceptions.DatastreamCannotLoadFileException;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamMimeView;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.DigitalObjectIdView;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseSearchService implements IIntegrationService {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;
  private final IDatastreamContentRepository datastreamContentRepository;
  private final IDublinCoreEntryRepository dublinCoreEntryRepository;


  private final SolrClient solrClient;

  @Override
  public void indexObjects(String projectAbbr) {
    log.trace("*** BaseSearchService: Indexing now project objects for: {}", projectAbbr);
    List<DigitalObjectIdView> digitalObjects = digitalObjectRepository.findAllByProject_ProjectAbbr(projectAbbr);
    digitalObjects.forEach(digitalObject -> indexObject(projectAbbr, digitalObject.getId()));
  }

  @Override
  public void deleteIndexedObjects(String projectAbbr) {
    log.trace("*** Trying to delete solr indexed project objects for: {}", projectAbbr);

    // delete selected from GAMS core
    solrClient.delete(SolrGamsCores.GAMS_CORE.value, String.format("%s:%s", BaseSearchProperties.PROJECT.name, projectAbbr));

    // delete all from project core
    solrClient.delete(projectAbbr, "*:*");

  }

  @Override
  public void indexObject(String projectAbbr, String id) {

    log.trace("*** BaseSearchService: Indexing now object with id {} for project {}", id, projectAbbr);

    DigitalObject digitalObject = digitalObjectRepository.findById(id)
            .orElseThrow(() -> new IntegrationDataProcessingException(String.format("Digital object with id %s not found", id)));

    BaseSearch baseSearch = new BaseSearch();

    var foundDatastreams = datastreamRepository.findAllDatastreamMimeViewsByDigitalObject(digitalObject);

    // id needs to stay the same -- otherwise multiple entries with same ids will be created.
    baseSearch.addProperty(BaseSearchProperties.OBJECT_ID.name, digitalObject.getId());
    baseSearch.addProperty(BaseSearchProperties.PROJECT.name, digitalObject.getProject().getProjectAbbr());
    baseSearch.addProperty(BaseSearchProperties.TYPE.name, BaseSearchTypes.DIGITAL_OBJECT.name);
    // index datastream ids
    if(!foundDatastreams.isEmpty()){
      baseSearch.addProperty(BaseSearchProperties.DATASTREAMS.name, foundDatastreams.stream().map(IDatastreamMimeView::getDsid).toList());
    }

    // These fields might differ from the dublin core!
     baseSearch.addProperty(BaseSearchProperties.TITLE.name, digitalObject.getBaseMetadata().getTitle());
     baseSearch.addProperty(BaseSearchProperties.DESCRIPTION.name, digitalObject.getBaseMetadata().getDescription());
     baseSearch.addProperty(BaseSearchProperties.CREATOR.name, digitalObject.getBaseMetadata().getCreator());
     baseSearch.addProperty(BaseSearchProperties.PUBLISHER.name, digitalObject.getPublisher());
     baseSearch.addProperty(BaseSearchProperties.RIGHTS.name, digitalObject.getBaseMetadata().getRights());


    // send datastream contained info to solr
    // based on conditions formulated by the datastream's metadata e.g. mimetype or dsid value, like DC.xml
    foundDatastreams.forEach(datastream -> {
      DatastreamId datastreamId =  DatastreamId.builder().dsid(datastream.getDsid()).digitalObject(id).build();
      // send custom search datastream directly to solr
      if(datastream.getDsid().equals(GAMSAPIntegrationDatastreamId.SEARCH_DATASTREAM_ID.name)) {
        sendCustomSolrDatastream(datastreamId, projectAbbr);
      }

      if(datastream.getDsid().equals(GAMSDsid.DC.getValue())){
        addDublinCore(baseSearch, datastreamId);
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
        baseSearch,
        DatastreamId.builder().digitalObject(digitalObject.getId()).dsid(fulltextDsid).build()
    );



    // the end post base search entity to SOLR
    solrClient.post(SolrGamsCores.GAMS_CORE.value, baseSearch);
    log.info("Successfully created SOLR document representing digital object {}", digitalObject.getId());

  }


  public String fulltextSearch(String projectAbbr, String searchTerm){
    // TODO implement
    // TODO sorting
    // TODO pagination
    // TODO additional stuff
    // TODO own issue?

    String response = solrClient.retrieveSolrDocumentByProperty(
        SolrGamsCores.GAMS_CORE.value,
        BaseSearchProperties.FULLTEXT.name,
        searchTerm
    );

    return response;

  }


  @Override
  public void deleteIndexedObject(String projectAbbr, String id) {

    // escape colons in id (goes through the webclient and solr)
    id = id.replaceAll(":", "\\\\\\\\:");

    // delete object from GAMS core
    solrClient.delete(SolrGamsCores.GAMS_CORE.value, String.format("%s:%s", BaseSearchProperties.OBJECT_ID.name, id));
    // this requires solr documents to have the projectAbbr field
    solrClient.delete(projectAbbr, String.format("%s:%s", BaseSearchProperties.OBJECT_ID.name, id));

  }


  /**
   * Sets up the solr integration service for the given project.
   * @param projectAbbr project abbreviation
   */
  public void setupIntegrationService(String projectAbbr){
    log.trace("*** Setting up integration service {}", this.getClass().getSimpleName());

    // check if the project setup is correct
    if (solrClient.coreExists(projectAbbr)){
      String msg = String.format("A solr core already exists for the project %s", projectAbbr);
      log.error(msg);
      throw new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }

   solrClient.createCore(projectAbbr);

  }

  /**
   * Adds dublin core field to given base search entity.
   * TODO test
   * @param baseSearch base search entity
   *                   (will be modified in place)
   * @param datastreamId datastream id
   */
  public void addDublinCore(BaseSearch baseSearch, DatastreamId datastreamId){
    var dcEntries = dublinCoreEntryRepository.findByDigitalObjectId(datastreamId.getDigitalObject());
    if(dcEntries.isEmpty()){
      String msg = String.format("No dublin core entries found for digital object %s", datastreamId.getDigitalObject());
      log.error(msg);
      throw new IntegrationDataProcessingException(msg);
    }
    dcEntries.forEach(dcEntry -> {
      String propertyName = "dc." + dcEntry.getName();
      String nodeValue = dcEntry.getValue();

      // if dc entry specifies a language -> prepend this e.g. 'en:'
      if((dcEntry.getLanguage()) != null && (!dcEntry.getLanguage().isEmpty())){
        nodeValue = dcEntry.getLanguage() + ":" +  nodeValue;
      }

      if(baseSearch.getProperty(propertyName) == null){
        baseSearch.addProperty(propertyName, List.of(nodeValue));
      } else {
        List<String> values = (List<String>) baseSearch.getProperty(propertyName);
        List<String> newValues = new ArrayList<>(values);
        newValues.add(nodeValue);
        baseSearch.addProperty(propertyName, newValues);
      }
    });

  }


  /**
   * Adds fulltext field to given base search entity.
   * TODO test
   * @param baseSearch base search entity
   * @param datastreamId datastream id
   */
  public void addFulltext(BaseSearch baseSearch, DatastreamId datastreamId){

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

    if(baseSearch.getProperty(BaseSearchProperties.FULLTEXT.name) == null){
      baseSearch.addProperty(BaseSearchProperties.FULLTEXT.name, docText);
    } else {
      String existingText = (String) baseSearch.getProperty(BaseSearchProperties.FULLTEXT.name);
      baseSearch.addProperty(BaseSearchProperties.FULLTEXT.name, existingText + "; " + docText  );
    }

  }

  /**
   * Sends custom solr datastream to solr.
   * TODO test?
   * @param datastreamId datastream id (object id and dsid)
   * @param projectAbbr project abbreviation
   */
  public void sendCustomSolrDatastream(DatastreamId datastreamId, String projectAbbr){
    InputStreamResource inputStreamResource =  datastreamContentRepository.findById(datastreamId);
    try {
      solrClient.post(projectAbbr, inputStreamResource.getContentAsByteArray());
    } catch (IOException e) {
      String msg = String.format("Failed to read datastream content %s for datastream %s. Original error: %s", inputStreamResource.getDescription(), datastreamId, e);
      log.error(msg);
      throw new DatastreamCannotLoadFileException(msg);
    }

  }



  /**
   * Performs faceted Dublin Core search in Solr.
   * Uses the updated schema with "dc.fieldname" format where all language variants
   * are stored in a single multi-valued field (e.g., dc.title = ["english title", "german title"])
   *
   * @param projectAbbrs Set of project abbreviations to filter by
   * @param selectedFacets MultiValueMap of selected Dublin Core facets (field -> values)
   * @param pageable Pagination information
   * @return FacetSearchResponse containing results, facets, and metadata
   */
  public BaseSearchFacetResponse facetSearch(
      Set<String> projectAbbrs,
      MultiValueMap<String, String> selectedFacets,
      Pageable pageable) {

    long startTime = System.currentTimeMillis();

    log.debug("Solr faceted search: projects={}, filters={}, page={}",
        projectAbbrs, selectedFacets, pageable);

    // Validate inputs
    if (projectAbbrs == null || projectAbbrs.isEmpty()) {
      throw new IntegrationDataProcessingException("Project abbreviations must not be empty");
    }

    // STEP 1: Build Solr query with filters
    String solrQuery = buildSolrFacetQuery(projectAbbrs, selectedFacets);

    // STEP 2: Define default facet fields (Dublin Core standard fields with "dc." prefix)
    // TODO: is this necessary?
    Set<String> facetFields = getDefaultDublinCoreFacetFields();

    // STEP 3: Execute Solr search with faceting
    String solrResponse = executeSolrFacetedSearch(
        SolrGamsCores.GAMS_CORE.value,
        solrQuery,
        facetFields,
        pageable
    );

    // STEP 4: Parse Solr response
    SolrFacetedResponse parsedResponse = SolrFacetedResponse.from(solrResponse);

    long totalTime = System.currentTimeMillis() - startTime;

    log.info("Solr faceted search completed in {}ms - found {} results with {} facet fields",
        totalTime, parsedResponse.getNumFound(), facetFields.size());


    // STEP 5: Transform to response from our API

    return BaseSearchFacetResponse.from(
        parsedResponse, selectedFacets
    );

  }

  /**
   * Builds Solr query string with project and Dublin Core filters.
   * Implements proper faceted search logic:
   * - Multiple values for SAME field = OR logic
   * - Different fields = AND logic
   */
  private String buildSolrFacetQuery(
      Set<String> projectAbbrs,
      MultiValueMap<String, String> selectedFacets) {

    List<String> queryParts = new ArrayList<>();

    // Add project filter (required)
    if (projectAbbrs.size() == 1) {
      queryParts.add(String.format("%s:%s",
          BaseSearchProperties.PROJECT.name,
          escapeSolrValue(projectAbbrs.iterator().next())));
    } else {
      String projectQuery = projectAbbrs.stream()
          .map(abbr -> String.format("%s:%s",
              BaseSearchProperties.PROJECT.name,
              escapeSolrValue(abbr)))
          .collect(Collectors.joining(" OR "));
      queryParts.add("(" + projectQuery + ")");
    }

    // Add Dublin Core facet filters
    if (selectedFacets != null && !selectedFacets.isEmpty()) {
      selectedFacets.forEach((dcField, values) -> {
        if (values != null && !values.isEmpty()) {
          // Map DC field name to Solr field name (dc.title, dc.creator, etc.)
          String solrFieldName = normalizeDublinCoreFieldName(dcField);

          // Build OR query for multiple values of same field
          if (values.size() == 1) {
            // Single value - simple query
            queryParts.add(buildSolrFieldQuery(solrFieldName, values.get(0)));
          } else {
            // Multiple values - OR query
            String fieldQuery = values.stream()
                .map(value -> buildSolrFieldQuery(solrFieldName, value))
                .collect(Collectors.joining(" OR "));
            queryParts.add("(" + fieldQuery + ")");
          }
        }
      });
    }

    // Combine all parts with AND
    String finalQuery = queryParts.isEmpty() ? "*:*" : String.join(" AND ", queryParts);

    log.debug("Built Solr query: {}", finalQuery);
    return finalQuery;
  }

  /**
   * Normalizes Dublin Core field names to Solr schema format.
   * Ensures consistent "dc.fieldname" format.
   *
   * Examples:
   * - "title" -> "dc.title"
   * - "dc.title" -> "dc.title"
   * - "creator" -> "dc.creator"
   */
  private String normalizeDublinCoreFieldName(String dcFieldName) {
    if (dcFieldName == null || dcFieldName.isEmpty()) {
      throw new IntegrationDataProcessingException("Dublin Core field name cannot be null or empty");
    }

    // Already has "dc." prefix
    if (dcFieldName.startsWith("dc.")) {
      return dcFieldName;
    }

    // Add "dc." prefix
    return "dc." + dcFieldName;
  }

  /**
   * Builds a Solr field query with proper escaping.
   * Handles multi-valued fields where all language variants are in one field.
   */
  private String buildSolrFieldQuery(String fieldName, String value) {
    if (value == null || value.trim().isEmpty()) {
      throw new IntegrationDataProcessingException("Search value cannot be null or empty");
    }

    String escapedValue = escapeSolrValue(value.trim());

    // For text fields, use exact phrase matching
    // This works well with multi-valued fields containing different language variants
    return String.format("%s:\"%s\"", fieldName, escapedValue);
  }

  /**
   * Escapes special characters in Solr query values.
   * CRITICAL: Must properly escape to prevent query syntax errors.
   */
  private String escapeSolrValue(String value) {
    if (value == null) {
      return "";
    }

    // Escape Solr special characters: + - && || ! ( ) { } [ ] ^ " ~ * ? : \ /
    return value
        .replace("\\", "\\\\")  // Backslash FIRST
        .replace("\"", "\\\"")  // Quote
        .replace("+", "\\+")
        .replace("-", "\\-")
        .replace("&&", "\\&&")
        .replace("||", "\\||")
        .replace("!", "\\!")
        .replace("(", "\\(")
        .replace(")", "\\)")
        .replace("{", "\\{")
        .replace("}", "\\}")
        .replace("[", "\\[")
        .replace("]", "\\]")
        .replace("^", "\\^")
        .replace("~", "\\~")
        .replace("*", "\\*")
        .replace("?", "\\?")
        .replace(":", "\\:");
  }

  /**
   * Returns default Dublin Core fields for faceting.
   * Uses schema format with "dc." prefix.
   *
   * Based on your schema definition and common faceting needs.
   */
  private Set<String> getDefaultDublinCoreFacetFields() {
    return Set.of(
        "dc.coverage",    // Geographic/temporal coverage - commonly faceted
        "dc.type",        // Resource type - commonly faceted
        "dc.creator",     // Creator/author - commonly faceted
        "dc.subject",     // Subject/keywords - commonly faceted
        "dc.language",    // Language - commonly faceted
        "dc.format",      // Format - commonly faceted
        "dc.publisher"    // Publisher - useful for faceting
    );
  }

  /**
   * Executes Solr faceted search query.
   * Returns raw JSON response from Solr.
   */
  private String executeSolrFacetedSearch(
      String coreName,
      String query,
      Set<String> facetFields,
      Pageable pageable) {

    // Build Solr request URL with faceting parameters
    StringBuilder url = new StringBuilder();
    url.append(String.format("/solr/%s/select", coreName));
    url.append("?q=").append(query);

    // Pagination
    url.append("&start=").append(pageable.getOffset());
    url.append("&rows=").append(pageable.getPageSize());

    // Sorting
    if (pageable.getSort().isSorted()) {
      String sortParam = pageable.getSort().stream()
          .map(order -> order.getProperty() + " " + order.getDirection().name().toLowerCase())
          .collect(Collectors.joining(","));
      url.append("&sort=").append(sortParam);
    }

    // Faceting parameters
    url.append("&facet=true");
    url.append("&facet.mincount=1"); // Only return facets with at least 1 doc
    url.append("&facet.limit=100");  // Max facet values per field
    url.append("&facet.sort=count"); // Sort by count (most common first)

    // Add facet fields
    for (String facetField : facetFields) {
      url.append("&facet.field=").append(facetField);
    }

    // Response format
    url.append("&wt=json");
    url.append("&indent=true");

    String finalUrl = url.toString();
    log.debug("Executing Solr faceted query: {}", finalUrl);

    return solrClient.get(finalUrl);
  }


}
