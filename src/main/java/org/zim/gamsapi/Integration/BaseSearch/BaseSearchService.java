package org.zim.gamsapi.Integration.BaseSearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.DatastreamId;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamDetailsView;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamIdView;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.Integration.Common.IntegrationActionReport;
import org.zim.gamsapi.Integration.Common.enums.GAMSAPIntegrationDatastreamId;
import org.zim.gamsapi.Integration.Common.enums.IntegrationActionStatus;
import org.zim.gamsapi.Integration.Common.enums.IntegrationActionType;
import org.zim.gamsapi.Integration.Common.exceptions.ProcessingException;
import org.zim.gamsapi.Integration.Common.interfaces.IIntegrationService;
import org.zim.gamsapi.System.configproperties.GAMSDockerDNS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseSearchService implements IIntegrationService {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;
  private final GAMSDockerDNS configProperties;

  private final String GAMS_CORE = "gams";

  // TODO elaborate usage of resttemplate
  private final RestTemplate restTemplate = new RestTemplate();

  private final SOLRClient solrClient;

  @Override
  public List<IntegrationActionReport> indexObjects(String projectAbbr) {
    List<IntegrationActionReport> integrationActionReports = new ArrayList<>();

    // TODO use simpler query (just digital object ids?)
    List<DigitalObject> digitalObjects = digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbr(projectAbbr);
    digitalObjects.forEach(digitalObject -> {
      log.trace("*** SOLR Indexing now object: {}", digitalObject);

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

      // TODO rename variable
      BaseSearch[] baseSearches = new BaseSearch[]{baseSearch};

      // TODO propper error handling?
      solrClient.postBaseSearchEntities(baseSearches, GAMS_CORE);
      log.info("Successfully created SOLR document representing digital object {}", digitalObject.getId());

      // TODO integration action reports missing

      try {
        IntegrationActionReport integrationActionReport = postSolrDatastream(digitalObject);
        integrationActionReports.add(integrationActionReport);
      } catch (ProcessingException e){
        // make sure that the indexing of the object is not interrupted by a failed post of the solr xml
        String msg = String.format("Failed indexing base search datastream for digital object %s. Root cause: %s", digitalObject.getId(), e);
        integrationActionReports.add(
                new IntegrationActionReport(projectAbbr, IntegrationActionType.INDEX_OBJECT, IntegrationActionStatus.ERROR, msg)
        );
      }

    });

    return integrationActionReports;
  }

  @Override
  public List<IntegrationActionReport> deleteIndexedObjects(String projectAbbr) {
    log.trace("*** Trying to delete solr indexed project objects for : {}", projectAbbr);

    // TODO think
    SolrClient client = getSolrClient(GAMS_CORE);
    String solrDeletionQuery = String.format("%s:%s", BaseSearchProperties.PROJECT.name, projectAbbr);
    try {
      client.deleteByQuery(solrDeletionQuery);
      client.commit();
      String msg = String.format("Committed SOLR delete all indexing operation for project %s via built solr-query %s", projectAbbr, solrDeletionQuery);
      log.info(msg);
      return List.of(new IntegrationActionReport(projectAbbr, IntegrationActionType.DELETE_OBJECT, IntegrationActionStatus.SUCCESS, msg));
    } catch (SolrServerException | IOException e){
      String msg = String.format("Failed to delete all solr documents for project %s", projectAbbr);
      log.error(msg);
      throw new ProcessingException(msg);
    }
  }

  @Override
  public List<IntegrationActionReport> indexObject(String projectAbbr, String id) {
    //TODO think about
    SolrClient client = getSolrClient("gams");
    DigitalObject digitalObject = digitalObjectRepository.findById(id)
            .orElseThrow(() -> new ProcessingException(String.format("Digital object with id %s not found", id)));

    log.trace("*** SOLR Indexing now object: {}", digitalObject.getId());
    SolrInputDocument solrInputDocument = createSolrInputDocument(digitalObject);

    List<IntegrationActionReport> indexingReports = new ArrayList<>();

    try {
      final UpdateResponse updateResponse = client.add(solrInputDocument);
      client.commit();
      String msg = String.format("Successfully SOLR indexed digital object representing document %s", digitalObject.getId());
      log.info(msg);
      indexingReports.add(
              new IntegrationActionReport(projectAbbr, IntegrationActionType.INDEX_OBJECT, IntegrationActionStatus.SUCCESS, msg)
      );
    } catch (SolrServerException | IOException e) {
      String msg = String.format("Failed indexation to SOLR of digital object %s . Original err msg: %s", digitalObject.getId(), e);
      log.error(msg);
      throw new ProcessingException(msg);
    }

    // posts custom search datastream
    try {
      IntegrationActionReport integrationActionReport = postSolrDatastream(digitalObject);
      indexingReports.add(integrationActionReport);
    } catch (ProcessingException e){
      String msg = String.format("Failed to index base search datastream of digital object %s. Root cause: %s", digitalObject.getId(), e);
      log.error(msg);
      indexingReports.add(new IntegrationActionReport(projectAbbr, IntegrationActionType.INDEX_OBJECT, IntegrationActionStatus.ERROR, msg));
    }

    return indexingReports;

  }

  @Override
  public List<IntegrationActionReport> deleteIndexedObject(String projectAbbr, String id) {

    // id might contain values that need to be escaped for solr
    // (otherwise a SOLRException would be thrown)
    id = id.replaceAll(":", "\\\\:");

    SolrClient client = getSolrClient(GAMS_CORE);
    String solrDeletionQuery = String.format("%s:%s", BaseSearchProperties.OBJECT_ID.name, id);
    try {
      client.deleteByQuery(solrDeletionQuery);
      client.commit();
      String msg = String.format("Committed SOLR delete object %s operation for project %s via built solr-query %s", id, projectAbbr, solrDeletionQuery);
      log.info(msg);
      return List.of(new IntegrationActionReport(projectAbbr, IntegrationActionType.DELETE_OBJECT, IntegrationActionStatus.SUCCESS, msg));
    } catch (SolrServerException | IOException e){
      String msg = String.format("Failed to delete all solr documents for digital object with id %s project %s", id, projectAbbr);
      log.error(msg);
      return List.of(new IntegrationActionReport(projectAbbr, IntegrationActionType.DELETE_OBJECT, IntegrationActionStatus.ERROR, msg));
    }
  }


  // TODO needs url as argument?
  public SolrClient getSolrClient(String coreName){
    final String solrUrl = configProperties.getBaseSearchUrl() + "/solr/" + coreName;
    return new HttpSolrClient.Builder(solrUrl)
            .build();
  }


  /**
   * Creates a solr input document from a digital object (does not include the solr datastream)
   * @param digitalObject digital object to be indexed
   * @return SolrInputDocument
   */
  private SolrInputDocument createSolrInputDocument(DigitalObject digitalObject){
    SolrInputDocument solrInputDocument = new SolrInputDocument();
    // id needs to stay the same -- otherwise multiple entries with same ids will be created.
    solrInputDocument.addField(BaseSearchProperties.ID.name, digitalObject.getId());
    solrInputDocument.addField(BaseSearchProperties.OBJECT_ID.name, digitalObject.getId());
    solrInputDocument.addField(BaseSearchProperties.PROJECT.name, digitalObject.getProject().getProjectAbbr());
    // index datastream ids
    // TODO refactor
    //solrInputDocument.addField(BaseSearchProperties.DATASTREAMS.name, digitalObject.getDatastreams().stream().map(Datastream::getDsid).collect(Collectors.toList()));
    solrInputDocument.addField(BaseSearchProperties.TYPE.name, BaseSearchTypes.DIGITAL_OBJECT.name);

    // index full text
    // TODO add missing validation (there must be a source_xml?)
    // TODO refactor
//    digitalObject.getDatastreams().stream().filter(datastream -> datastream.getDsid().equals(GAMSAPIntegrationDatastreamId.SOURCE_DATASTREAM_ID.name)).forEach(datastream -> {
//      String fulltext = XMLUtils.extractText(XMLUtils.parseXml(datastream.getData()));
//      solrInputDocument.addField(BaseSearchProperties.FULLTEXT.name, fulltext);
//    });
    return solrInputDocument;
  }


  /**
   * Posts a custom solr datastream to the solr instance via a post request if available
   * and valid.
   * TODO redo implementation?
   * @param digitalObject digital object to be indexed
   */
  private IntegrationActionReport postSolrDatastream(DigitalObject digitalObject) throws ProcessingException {
    // TODO use project abbreviation of digital object to determine the core

    // if no core exists for the project, abort
    if(!solrClient.coreExists(digitalObject.getProject().getProjectAbbr())){
      String msg = String.format("No solr core found for project %s", digitalObject.getProject().getProjectAbbr());
      log.error(msg);
      // TODO better exception here
      throw new ProcessingException(msg);
    }

    var objectDatastreams =  datastreamRepository.findAllByDigitalObjectId(digitalObject.getId());
    if(objectDatastreams.isEmpty()){
      String msg = String.format("No datastreams found for digital object %s", digitalObject.getId());
      log.debug(msg);
      return new IntegrationActionReport(digitalObject.getProject().getProjectAbbr(), IntegrationActionType.INDEX_OBJECT, IntegrationActionStatus.SKIPPED, msg);
    }

    Optional<IDatastreamDetailsView> searchDatastreamOptional = objectDatastreams.stream()
        .filter(datastream -> datastream.getDsid().equals(GAMSAPIntegrationDatastreamId.SEARCH_DATASTREAM_ID.name))
        .findFirst();

    if(searchDatastreamOptional.isEmpty()){
      String msg = String.format("No search datastream found for digital object %s", digitalObject.getId());
      log.debug(msg);
      return new IntegrationActionReport(digitalObject.getProject().getProjectAbbr(), IntegrationActionType.INDEX_OBJECT, IntegrationActionStatus.SKIPPED, msg);
    }

    var searchDatastream = searchDatastreamOptional.get();
    Datastream datastream = datastreamRepository.findById(DatastreamId.builder().dsid(searchDatastream.getDsid()).digitalObject(searchDatastream.getDigitalObject().getId()).build())
        .orElseThrow(() -> {
          String msg = String.format("Datastream with dsid %s not found", searchDatastream.getDsid());
          log.error(msg);
          return new ProcessingException(msg);
        });

    // TODO do i really need to parse the datastream? (not enough to just send along the data?)
    BaseSearch[] facets;
    ObjectMapper objectMapper = new ObjectMapper();

    try {
      facets = objectMapper.readValue(datastream.getData(), BaseSearch[].class);
    } catch (IOException e) {
      String msg = String.format("Failed to parse custom solr datastream to solr. Digital object: %s Cause: %s Original error message: %s", digitalObject.getId(), e.getMessage(), e);
      log.error(msg);
      throw new ProcessingException(msg);
    }

    solrClient.postBaseSearchEntities(facets, digitalObject.getProject().getProjectAbbr());

    // TODO refactor building of the integration action report
    return new IntegrationActionReport(digitalObject.getProject().getProjectAbbr(), IntegrationActionType.INDEX_OBJECT, IntegrationActionStatus.SUCCESS, "Successfully posted custom search xml datastream for object to solr instance.");
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
