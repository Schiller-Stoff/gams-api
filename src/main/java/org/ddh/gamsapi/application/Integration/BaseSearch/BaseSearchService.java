package org.ddh.gamsapi.application.Integration.BaseSearch;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrClient;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrGamsCores;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
      // TODO think about disabled custom solr indexing
//      if(datastream.getDsid().equals(GAMSAPIntegrationDatastreamId.SEARCH_DATASTREAM_ID.name)) {
//        sendCustomSolrDatastream(datastreamId, projectAbbr);
//      }

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
        nodeValue = dcEntry.getLanguage() + ": " +  nodeValue;
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
   * Indexes all digital objects for a given project that have FULLTEXT_INDEX.json datastream.
   * @param projectAbbr project abbreviation
   */
  public void indexCustom(String projectAbbr){

    log.info("*** BaseSearchService: Starting project indexing for: {}", projectAbbr);

    List<DigitalObjectIdView> digitalObjects = digitalObjectRepository.findAllByProject_ProjectAbbr(projectAbbr);

    if (digitalObjects.isEmpty()) {
      log.info("No digital objects found for project: {}", projectAbbr);
      return;
    }

    log.info("Found {} digital objects for project {}", digitalObjects.size(), projectAbbr);

    List<DatastreamId> objectIdsWithFulltext = new ArrayList<>();

    // TODO instead use method on datastream repository to find all datastreams that match FULLTEXT_INDEX.json
    digitalObjects.forEach(digitalObject -> {
      var datastreamId = DatastreamId.builder()
          .digitalObject(digitalObject.getId())
          .dsid("FULLTEXT_INDEX.json")
          .build();
      if(datastreamRepository.existsById(datastreamId)){
        objectIdsWithFulltext.add(datastreamId);
      }
    });

    /////
    //
    // BATCH INDEX ALL fulltext documents together

    if(objectIdsWithFulltext.isEmpty()){
      log.info("No digital objects with FULLTEXT_INDEX.json datastream found for project: {}", projectAbbr);
      return;
    }

    int batchCount = 0;
    int objectsProcessed = 0;

    // TODO this should be configurable?
    final int DEFAULT_BATCH_SIZE = 500;


    List<BaseSearch> currentBatch = new ArrayList<>(500);
    for (DatastreamId datastreamId : objectIdsWithFulltext) {
      objectsProcessed++;

      // load datastream content
      InputStreamResource resource = datastreamContentRepository.findById(datastreamId);

      // parse datastream content as BaseSearch array
      BaseSearch[] baseSearch;
      try {
        baseSearch = BaseSearch.from(resource);
      } catch (IOException e) {
        String msg = String.format("Failed to read datastream content %s for datastream %s. Original error: %s", resource.getDescription(), datastreamId, e);
        log.error(msg);
        // TODO better / different exception
        throw new DatastreamCannotLoadFileException(msg);
      }

      // TODO validate baseSearch entries?

      //02. add to batch
      // TODO fix - inefficient array to list conversion - and back
      currentBatch.addAll(Arrays.asList(baseSearch));

      //03. After reaching batch size, post to solr + clear batch (with commit)
      // Flush batch if it's getting full (but don't commit yet)
      if (currentBatch.size() >= DEFAULT_BATCH_SIZE) {
        log.info("Processed {} BaseSearch objects from datastream {} - current batch: {}", currentBatch.size(), datastreamId, currentBatch);
        solrClient.post(SolrGamsCores.FULLTEXT_CORE.value, currentBatch.toArray(new BaseSearch[0]));
        currentBatch.clear();
        batchCount++;
        log.info("Processed batch {} for fulltext core ({} objects processed so far)", batchCount, objectsProcessed);
      }

    }

    // Post remaining documents in batch (e.g. when batch size not reached at the end / or never reached)
    if (!currentBatch.isEmpty()) {
      solrClient.post(SolrGamsCores.FULLTEXT_CORE.value, currentBatch.toArray(new BaseSearch[0]));
    }



    // TODO think about - do i really need to commit by hand?
    // Final commit
//    try {
//      solrClient.commit(SolrGamsCores.FULLTEXT_CORE.value);
//      log.info("Final commit completed for fulltext core");
//    } catch (Exception e) {
//      //stats.addWarning("Final commit failed: " + e.getMessage());
//    }



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

}
