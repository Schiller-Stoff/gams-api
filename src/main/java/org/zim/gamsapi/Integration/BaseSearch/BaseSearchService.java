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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.Integration.Common.IntegrationActionReport;
import org.zim.gamsapi.Integration.Common.enums.IntegrationActionStatus;
import org.zim.gamsapi.Integration.Common.enums.IntegrationActionType;
import org.zim.gamsapi.Integration.Common.enums.GAMSAPIntegrationDatastreamId;
import org.zim.gamsapi.Integration.Common.exceptions.ProcessingException;
import org.zim.gamsapi.Integration.Common.interfaces.IIntegrationService;
import org.zim.gamsapi.Integration.Common.utils.XMLUtils;
import org.zim.gamsapi.System.configproperties.GAMSDockerDNS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseSearchService implements IIntegrationService {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final GAMSDockerDNS configProperties;

  private final String GAMS_CORE = "gams";

  // TODO elaborate usage of resttemplate
  private final RestTemplate restTemplate = new RestTemplate();

  @Override
  public List<IntegrationActionReport> indexObjects(String projectAbbr) {

    SolrClient client = getSolrClient(GAMS_CORE);
    List<IntegrationActionReport> integrationActionReports = new ArrayList<>();

    List<DigitalObject> digitalObjects = digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbr(projectAbbr);
    digitalObjects.forEach(digitalObject -> {
      log.trace("*** SOLR Indexing now object: {}", digitalObject);
      SolrInputDocument solrInputDocument = createSolrInputDocument(digitalObject);
      try {
        final UpdateResponse updateResponse = client.add(solrInputDocument);
        String msg = String.format("Successfully created SOLR document representing digital object %s", digitalObject.getId());
        log.info(msg);
        integrationActionReports.add(
                new IntegrationActionReport(projectAbbr, IntegrationActionType.INDEX_OBJECT, IntegrationActionStatus.SUCCESS, msg)
        );
      } catch (SolrServerException | IOException e) {
        String msg = String.format("Failed indexation to SOLR of digital object %s . Original err msg: %s", digitalObject.getId(), e);
        log.error(msg);
        // abort complete operation if digital object document cannot be created.
        throw new ProcessingException(msg);
      }

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

    try {
      client.commit();
      String msg = String.format("Successfully committed SOLR indexing operation for project %s", projectAbbr);
      log.info(msg);
      return integrationActionReports;
    } catch (SolrServerException | IOException e) {
      String msg = String.format("Failed to commit SOLR indexing operation for project %s . Original error message: %s", projectAbbr, e);
      log.error(msg);
      throw new ProcessingException(msg);
    }
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
    final String solrUrl = configProperties.getBaseSearchUrl() + "/" + coreName;
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

    // TODO reimplement

//    Optional<Datastream> datastreamOptional = digitalObject.getDatastreams().stream().filter(dstream -> dstream.getDsid().equals(GAMSAPIntegrationDatastreamId.SEARCH_DATASTREAM_ID.name)).findFirst();
//    Datastream datastream;
//    if(datastreamOptional.isEmpty()) {
//      // if no search.json - skip processing
//      String msg = String.format("Skipped indexing custom search datastream because none found at digital object %s", digitalObject.getId());
//      log.debug(msg);
//      return new IntegrationActionReport(digitalObject.getProject().getProjectAbbr(), IntegrationActionType.INDEX_OBJECT, IntegrationActionStatus.SKIPPED, msg);
//    } else {
//      datastream = datastreamOptional.get();
//    }
//
//    BaseSearch[] facets;
//    ObjectMapper objectMapper = new ObjectMapper();
//
//    try {
//      facets = objectMapper.readValue(datastream.getData(), BaseSearch[].class);
//    } catch (IOException e){
//      String msg = String.format("Failed to parse custom solr datastream to solr. Digital object: %s Cause: %s Original error message: %s", digitalObject.getId(), e.getMessage(), e);
//      log.error(msg);
//      throw new ProcessingException(msg);
//    }
//
//    // ensures that each solr entity = document has gams-controlled properties assigned
//    Arrays.stream(facets).forEach(facet -> {
//      facet.properties.put(BaseSearchProperties.OBJECT_ID.name, digitalObject.getId());
//      facet.properties.put(BaseSearchProperties.PROJECT.name, digitalObject.getProject().getProjectAbbr());
//      // id must be defined outside
//    });
//
//    String builtJson = "";
//    try {
//      builtJson = objectMapper.writeValueAsString(facets);
//    } catch (JsonProcessingException e){
//      String msg = String.format("Failed to marshal Facet objects to json array. Skipping solr indexing. Digital object %s . Cause: %s Original error message: %s", digitalObject.getId(), e.getMessage(), e);
//      log.error(msg);
//      throw new ProcessingException(msg);
//    }
//
//    log.info("Built json: {}", builtJson);
//
//    // TODO need to block sending of json if a add document operation.
//    // TODO this json needs some kind of validation e.g. every doc must have a projectAbbreviation assigned etc.
//
//    HttpHeaders httpHeaders = new HttpHeaders();
//    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
//    HttpEntity<String> request = new HttpEntity<>(builtJson, httpHeaders);
//
//    //TODO improve handling of RestClientException?
//    ResponseEntity<String> response;
//    try {
//      String postUrl = String.format("%s/update/json/docs?commit=true", configProperties.getBaseSearchUrl());
//      response = restTemplate.postForEntity(postUrl, request, String.class);
//    } catch (RestClientException e){
//      String msg = String.format("Failed to post custom solr datastream to solr instance. Digital object: %s Cause: %s Original error message: %s", digitalObject.getId(), e.getMessage(), e);
//      log.error(msg);
//      throw new ProcessingException(msg);
//    }
//
//    if(response.getStatusCode().isError()){
//      String msg = String.format("Failed to post custom solr datastream to solr instance for object %s Response status code: %s", digitalObject.getId(), response.getStatusCode());
//      log.error(msg);
//      throw new ProcessingException(msg);
//    } else {
//      String msg = String.format("Successfully posted custom search xml datastream for object %s to solr instance. Response status code: %s", digitalObject.getId(), response.getStatusCode());
//      log.trace(msg);
//      return new IntegrationActionReport(digitalObject.getProject().getProjectAbbr(), IntegrationActionType.INDEX_OBJECT, IntegrationActionStatus.SUCCESS, msg);
//    }

    return null;

  }

  /**
   * Sets up the solr integration service for the given project.
   * @param projectAbbr project abbreviation
   */
  public void setupIntegrationService(String projectAbbr){
    log.trace("*** Setting up integration service {}", this.getClass().getSimpleName());

    // TODO refactor using webclient
    RestTemplate restTemplate = new RestTemplate();
    // TODO refactor - connection consideartions (retry / timeout / etc. )


    String coreStatusUrl = configProperties.getBaseSearchUrl() + "/" + projectAbbr + "/select";

    // proceed only if the core doesn't exist
    try {
      var responseEntity = restTemplate.exchange(coreStatusUrl, HttpMethod.GET, null, String.class);
      // abort if core already exists
      if(responseEntity.getStatusCode().equals(HttpStatus.OK)){
        String msg = String.format("A solr core already exists for the project %s", projectAbbr);
        log.error(msg);
        throw new ResponseStatusException(HttpStatus.CONFLICT, msg);
      }
    } catch (HttpClientErrorException e){
      // proceed only if the error indicates not found in the status code
      if(e.getStatusCode() != HttpStatus.NOT_FOUND){
        String msg = String.format("Something went wrong requesting status of the solr core for project %s", projectAbbr);
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, msg);
      }
    }


    // request against SOLR to create the project core

    String body = String.format("""
          {
              "create": {
                "name": "%s",
                "configSet": "base"
              }
            }
        """, projectAbbr);


    // TODO replace localhost - must be dynamic (controlled by config)
    String url = "http://localhost:8983/api/cores";

    try {
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<String> httpEntity = new HttpEntity<>(body, httpHeaders);
      restTemplate.postForEntity(url, httpEntity, String.class);
      //restTemplate.exchange(coreStatusUrl, HttpMethod.POST, httpEntity, String.class);
    } catch (HttpClientErrorException e){
      String msg = String.format("Something went wrong creating the solr core for project %s", projectAbbr);
      log.error(msg);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, msg);
    }



    // TODO efficient this way?
//    WebClient webClient = WebClient.create();
//
//
//    String response = webClient
//        .post()
//        .uri(url)
//        .contentType(MediaType.APPLICATION_JSON)
//        // important to use body inserters here
//        .body(BodyInserters.fromValue(body))
//        .retrieve()
//        .toEntity(String.class)
//        .doOnError(throwable -> log.error("Error while setting up integration service", throwable.getCause()))
//        // TODO use subscribe instead?
//        .block()
//        .getBody();


  }


}
