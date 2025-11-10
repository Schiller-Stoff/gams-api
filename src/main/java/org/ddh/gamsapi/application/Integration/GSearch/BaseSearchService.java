package org.ddh.gamsapi.application.Integration.GSearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationDataProcessingException;
import org.ddh.gamsapi.application.Integration.Common.interfaces.IIntegrationService;
import org.ddh.gamsapi.application.Integration.Common.utils.XMLUtils;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrClient;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrDocument;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrGamsCores;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.ArrayList;
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

  }

  @Override
  public void indexObject(String projectAbbr, String id) {

    log.trace("*** BaseSearchService: Indexing now object with id {} for project {}", id, projectAbbr);

    DigitalObject digitalObject = digitalObjectRepository.findById(id)
            .orElseThrow(() -> new IntegrationDataProcessingException(String.format("Digital object with id %s not found", id)));

    SolrDocument solrDocument = new SolrDocument();

    var foundDatastreams = datastreamRepository.findAllDatastreamMimeViewsByDigitalObject(digitalObject);

    // id needs to stay the same -- otherwise multiple entries with same ids will be created.
    solrDocument.addProperty(BaseSearchProperties.OBJECT_ID.name, digitalObject.getId());
    solrDocument.addProperty(BaseSearchProperties.PROJECT.name, digitalObject.getProject().getProjectAbbr());
    solrDocument.addProperty(BaseSearchProperties.TYPE.name, BaseSearchTypes.DIGITAL_OBJECT.name);
    // index datastream ids
    if(!foundDatastreams.isEmpty()){
      solrDocument.addProperty(BaseSearchProperties.DATASTREAMS.name, foundDatastreams.stream().map(IDatastreamMimeView::getDsid).toList());
    }

    // These fields might differ from the dublin core!
     solrDocument.addProperty(BaseSearchProperties.TITLE.name, digitalObject.getBaseMetadata().getTitle());
     solrDocument.addProperty(BaseSearchProperties.DESCRIPTION.name, digitalObject.getBaseMetadata().getDescription());
     solrDocument.addProperty(BaseSearchProperties.CREATOR.name, digitalObject.getBaseMetadata().getCreator());
     solrDocument.addProperty(BaseSearchProperties.PUBLISHER.name, digitalObject.getPublisher());
     solrDocument.addProperty(BaseSearchProperties.RIGHTS.name, digitalObject.getBaseMetadata().getRights());


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
    solrClient.post(SolrGamsCores.GAMS_CORE.value, solrDocument);
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
   * @param solrDocument base search entity
   *                   (will be modified in place)
   * @param datastreamId datastream id
   */
  public void addDublinCore(SolrDocument solrDocument, DatastreamId datastreamId){
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

      if(solrDocument.getProperty(propertyName) == null){
        solrDocument.addProperty(propertyName, List.of(nodeValue));
      } else {
        List<String> values = (List<String>) solrDocument.getProperty(propertyName);
        List<String> newValues = new ArrayList<>(values);
        newValues.add(nodeValue);
        solrDocument.addProperty(propertyName, newValues);
      }
    });

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

    if(solrDocument.getProperty(BaseSearchProperties.FULLTEXT.name) == null){
      solrDocument.addProperty(BaseSearchProperties.FULLTEXT.name, docText);
    } else {
      String existingText = (String) solrDocument.getProperty(BaseSearchProperties.FULLTEXT.name);
      solrDocument.addProperty(BaseSearchProperties.FULLTEXT.name, existingText + "; " + docText  );
    }

  }

}
