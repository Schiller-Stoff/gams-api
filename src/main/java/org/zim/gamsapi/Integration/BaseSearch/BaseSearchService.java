package org.zim.gamsapi.Integration.BaseSearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.zim.gamsapi.Datastream.DatastreamId;
import org.zim.gamsapi.Datastream.GAMSDsid;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamRepository;
import org.zim.gamsapi.Datastream.exceptions.DatastreamCannotLoadFileException;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamMimeView;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.zim.gamsapi.DigitalObject.utils.interfaces.DigitalObjectIdView;
import org.zim.gamsapi.Integration.Common.enums.GAMSAPIntegrationDatastreamId;
import org.zim.gamsapi.Integration.Common.exceptions.IntegrationDataProcessingException;
import org.zim.gamsapi.Integration.Common.interfaces.IIntegrationService;
import org.zim.gamsapi.Integration.Common.utils.XMLUtils;
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

  private final String GAMS_CORE = "gams";


  private final SOLRClient solrClient;

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
    solrClient.delete(GAMS_CORE, String.format("%s:%s", BaseSearchProperties.PROJECT.name, projectAbbr));

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

      // decide based on mimetype which documents to index
      if(datastream.getMimeType().contains("xml")){
        addFulltext(baseSearch, datastreamId);
      }


    });

    // the end post base search entity to SOLR
    solrClient.post(GAMS_CORE, baseSearch);
    log.info("Successfully created SOLR document representing digital object {}", digitalObject.getId());

  }

  @Override
  public void deleteIndexedObject(String projectAbbr, String id) {

    // escape colons in id (goes through the webclient and solr)
    id = id.replaceAll(":", "\\\\\\\\:");

    // delete object from GAMS core
    solrClient.delete(GAMS_CORE, String.format("%s:%s", BaseSearchProperties.OBJECT_ID.name, id));
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
    var dcContent =  datastreamContentRepository.findById(datastreamId);
    Document dcXml;
    try {
      dcXml = XMLUtils.parseXml(dcContent.getInputStream());
    } catch (IOException e) {
      String msg = String.format("Failed to read datastream content %s for datastream %s. Original error: %s", dcContent.getDescription(), datastreamId, e);
      log.error(msg);
      throw new DatastreamCannotLoadFileException(msg);
    }

    // retrieve all child elements of the root element
    // TODO validate if it's correct dublin core?
    // TODO this might be risky (will index all elements in the xml file)
    var dcNodes = XMLUtils.getAllXpath("/*/*", dcXml);


    for (int i = 0; i < dcNodes.getLength(); i++) {
      var node = dcNodes.item(i);
      String nodeName = node.getNodeName().replace(":", "_"); // solr recommends not to use colons in field names
      String nodeValue = node.getTextContent();

      // assign dynamic field for every dc element
      String solrPostfix = "_ss";
      // map lang attribute to solr if available
      // TODO sophisticate handling of dublin core lang attribute
      try {
        String langAttributeValue = XMLUtils.extractAttributeValue("xml:lang", node);
        solrPostfix = "_lang_" + langAttributeValue + solrPostfix;
      } catch (IntegrationDataProcessingException e){
        // no lang attribute found
        log.trace("No lang attribute found for dublin core element {}", nodeName);
      }

      String propertyName = nodeName + solrPostfix;
      // add possible multiple values for the same field
      if(baseSearch.getProperty(propertyName) == null){
        baseSearch.addProperty(propertyName, List.of(nodeValue));
      } else {
        List<String> values = (List<String>) baseSearch.getProperty(propertyName);
        List<String> newValues = new ArrayList<>(values);
        newValues.add(nodeValue);
        baseSearch.addProperty(propertyName, newValues);
      }
    }

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


}
