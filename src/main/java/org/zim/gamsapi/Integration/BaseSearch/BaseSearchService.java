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
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.Datastream.exceptions.DatastreamCannotLoadFileException;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamIdView;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectIdView;
import org.zim.gamsapi.Integration.Common.enums.GAMSAPIntegrationDatastreamId;
import org.zim.gamsapi.Integration.Common.exceptions.IntegrationDataProcessingException;
import org.zim.gamsapi.Integration.Common.interfaces.IIntegrationService;
import org.zim.gamsapi.Integration.Common.utils.XMLUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    var foundDatastreams = datastreamRepository.findAllDatastreamIdViewsByDigitalObject(digitalObject);

    // id needs to stay the same -- otherwise multiple entries with same ids will be created.
    baseSearch.addProperty(BaseSearchProperties.OBJECT_ID.name, digitalObject.getId());
    baseSearch.addProperty(BaseSearchProperties.PROJECT.name, digitalObject.getProject().getProjectAbbr());
    baseSearch.addProperty(BaseSearchProperties.TYPE.name, BaseSearchTypes.DIGITAL_OBJECT.name);
    // index datastream ids
    if(!foundDatastreams.isEmpty()){
      baseSearch.addProperty(BaseSearchProperties.DATASTREAMS.name, foundDatastreams.stream().map(IDatastreamIdView::getDsid).toList());
    }

    baseSearch.addProperty(BaseSearchProperties.TITLE.name, digitalObject.getBaseMetadata().getTitle());
    baseSearch.addProperty(BaseSearchProperties.DESCRIPTION.name, digitalObject.getBaseMetadata().getDescription());
    baseSearch.addProperty(BaseSearchProperties.CREATOR.name, digitalObject.getBaseMetadata().getCreator());
    baseSearch.addProperty(BaseSearchProperties.PUBLISHER.name, digitalObject.getPublisher());
    baseSearch.addProperty(BaseSearchProperties.RIGHTS.name, digitalObject.getBaseMetadata().getRights());


    // Translate dublin core to solr fields (BaseSearchEntity)
    // TODO fix doubled GAMS-datastream id static-classes! (BaseSearchProperties and GamsDatastreamIds)
    foundDatastreams.forEach(datastream -> {
      if(datastream.getDsid().equals(GAMSAPIntegrationDatastreamId.SEARCH_DATASTREAM_ID.name)) return;

      if(datastream.getDsid().equals(GAMSDsid.DC.getValue())){
        var dcContent =  datastreamContentRepository.findById(DatastreamId.builder().digitalObject(id).dsid(GAMSDsid.DC.getValue()).build());
        byte[] content;
        try {
          content = dcContent.getContentAsByteArray();
        } catch (IOException e) {
          String msg = String.format("Failed to read datastream content %s for datastream %s. Original error: %s", dcContent.getDescription(), datastream, e);
          log.error(msg);
          throw new DatastreamCannotLoadFileException(msg);
        }

        Document dcXml = XMLUtils.parseXml(content);

        // retrieve all child elements of dublin core root element
        var dcNodes = XMLUtils.getAllXpath("/*/*", dcXml);

        // TODO think about attributes on dublin core e.g. for the language.

        for (int i = 0; i < dcNodes.getLength(); i++) {
          var node = dcNodes.item(i);
          String nodeName = node.getNodeName().replace(":", "_"); // solr recommends not to use colons in field names
          String nodeValue = node.getTextContent();

          // assign dynamic field for every dc element
          String solrPostfix = "_ss";
          // map lang attribute to solr if available
          try {
            String langAttributeValue = XMLUtils.extractAttributeValue("xml:lang", node);
            solrPostfix = "_lang_" + langAttributeValue + solrPostfix;
          } catch (IntegrationDataProcessingException e){
            // no lang attribute found
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

    });

    // the end post base search entity to SOLR
    solrClient.post(GAMS_CORE, baseSearch);
    log.info("Successfully created SOLR document representing digital object {}", digitalObject.getId());


    //***** from here post the custom solr datastream

    // TODO this check is outdated? (because: i have already a list of datastreams available)
    // TODO AND: querying against datastreamRepository is also not necessary?
    // if no search datastream was found, do nothing
    Optional<IDatastreamIdView> datastreamIdViewOptional = foundDatastreams.stream()
        .filter(datastream -> datastream.getDsid().equals(GAMSAPIntegrationDatastreamId.SEARCH_DATASTREAM_ID.name))
        .findFirst();
    if(datastreamIdViewOptional.isEmpty())return;
    IDatastreamIdView datastreamIdView = datastreamIdViewOptional.get();
    DatastreamId datastreamId = DatastreamId.builder().dsid(datastreamIdView.getDsid()).digitalObject(id).build();


    datastreamRepository
        .findById(datastreamId)
        .ifPresentOrElse(datastream -> {
          InputStreamResource inputStreamResource =  datastreamContentRepository.findById(datastreamId);
          byte[] content;
          try {
            content = inputStreamResource.getContentAsByteArray();
          } catch (IOException e) {
            String msg = String.format("Failed to read datastream content %s for datastream %s. Original error: %s", inputStreamResource.getDescription(), datastream, e);
            log.error(msg);
            throw new DatastreamCannotLoadFileException(msg);
          }
          solrClient.post(projectAbbr, content);
        }, () -> {
          String msg = String.format("Unexpectedly failed to retrieve search datastream with dsid %s for digital object with id %s", datastreamIdView.getDsid(), id);
          log.error(msg);
          throw new IntegrationDataProcessingException("Datastream with dsid " + datastreamIdView.getDsid() + " not found at object with id " + id + ".");
        });

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

}
