package org.zim.gamsapi.Integration.BaseSearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.DatastreamId;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.Datastream.exceptions.DatastreamNotFoundException;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamIdView;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectIdView;
import org.zim.gamsapi.Integration.Common.enums.GAMSAPIntegrationDatastreamId;
import org.zim.gamsapi.Integration.Common.exceptions.ProcessingException;
import org.zim.gamsapi.Integration.Common.interfaces.IIntegrationService;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseSearchService implements IIntegrationService {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;

  private final String GAMS_CORE = "gams";


  private final SOLRClient solrClient;

  @Override
  public void indexObjects(String projectAbbr) {
    List<DigitalObjectIdView> digitalObjects = digitalObjectRepository.findAllByProject_ProjectAbbr(projectAbbr);
    digitalObjects.forEach(digitalObject -> {
      indexObject(projectAbbr, digitalObject.getId());
    });
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

    DigitalObject digitalObject = digitalObjectRepository.findById(id)
            .orElseThrow(() -> new ProcessingException(String.format("Digital object with id %s not found", id)));

    BaseSearch baseSearch = new BaseSearch();

    var foundDatastreams = datastreamRepository.findAllDatastreamIdViewsByDigitalObject(digitalObject);

    // id needs to stay the same -- otherwise multiple entries with same ids will be created.
    baseSearch.addProperty(BaseSearchProperties.ID.name, digitalObject.getId());
    baseSearch.addProperty(BaseSearchProperties.OBJECT_ID.name, digitalObject.getId());
    baseSearch.addProperty(BaseSearchProperties.PROJECT.name, digitalObject.getProject().getProjectAbbr());
    baseSearch.addProperty(BaseSearchProperties.TYPE.name, BaseSearchTypes.DIGITAL_OBJECT.name);
    // index datastream ids
    if(!foundDatastreams.isEmpty()){
      // TODO if this is built incorrectly - webclient error messages are just cryptic
      baseSearch.addProperty(BaseSearchProperties.DATASTREAMS.name, foundDatastreams.stream().map(IDatastreamIdView::getDsid).toList());
    }

    // TODO propper error handling?
    solrClient.post(GAMS_CORE, baseSearch);
    log.info("Successfully created SOLR document representing digital object {}", digitalObject.getId());

    // TODO integration action reports missing

    //***** from here post the custom solr datastream


    // posts custom search datastream
    try {
      Datastream searchDatastream = loadSearchDatastream(foundDatastreams, digitalObject.getId());
      solrClient.post(projectAbbr, searchDatastream.getData());
    } catch (DatastreamNotFoundException e){
      String msg = String.format("No search datastream found for digital object %s", digitalObject.getId());
      log.trace(msg);
    }

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
   * Loads the search datastream for a given digital object.
   * @param datastreamIdViews list of datastream id views
   * @param objectId id of the digital object
   * @return search datastream
   * @throws DatastreamNotFoundException if no search datastream was found
   */
  private Datastream loadSearchDatastream(List<IDatastreamIdView> datastreamIdViews, String objectId){

    IDatastreamIdView datastreamIdView = datastreamIdViews.stream()
        .filter(datastream -> datastream.getDsid().equals(GAMSAPIntegrationDatastreamId.SEARCH_DATASTREAM_ID.name))
        .findFirst()
        .orElseThrow(() -> new DatastreamNotFoundException("No search datastream found for digital object with id " + objectId));

    return datastreamRepository.findById(DatastreamId.builder().dsid(datastreamIdView.getDsid()).digitalObject(objectId).build())
        .orElseThrow(() -> {
          // TODO better message + logging
          return new ProcessingException("Datastream with dsid " + datastreamIdView.getDsid() + " not found at object with id " + objectId + ".");
        });

  }


}
