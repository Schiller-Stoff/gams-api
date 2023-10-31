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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
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

  // TODO elaborate usage of resttemplate
  private final RestTemplate restTemplate = new RestTemplate();

  @Override
  public List<IntegrationActionReport> indexObjects(String projectAbbr) {

    SolrClient client = getSolrClient();
    List<IntegrationActionReport> facetsDatastreamReports = new ArrayList<>();

    List<DigitalObject> digitalObjects = digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbr(projectAbbr);
    digitalObjects.forEach(digitalObject -> {
      log.trace("*** SOLR Indexing now object: {}", digitalObject);
      SolrInputDocument solrInputDocument = createSolrInputDocument(digitalObject);
      try {
        final UpdateResponse updateResponse = client.add(solrInputDocument);
        String msg = String.format("Successfully created SOLR document representing digital object %s", digitalObject.getId());
        log.info(msg);
        facetsDatastreamReports.add(
                new IntegrationActionReport(projectAbbr, IntegrationActionType.INDEX_OBJECT, IntegrationActionStatus.SUCCESS, msg)
        );
      } catch (SolrServerException | IOException e) {
        String msg = String.format("Failed indexation to SOLR of digital object %s . Original err msg: %s", digitalObject.getId(), e);
        log.error(msg);
        // abort complete operation if digital object document cannot be created.
        throw new ProcessingException(msg);
      }

      try {
        postSolrDatastream(digitalObject);
        facetsDatastreamReports.add(
                new IntegrationActionReport(projectAbbr, IntegrationActionType.INDEX_OBJECT, IntegrationActionStatus.SUCCESS,"Indexed facets datastream for digital object " + digitalObject.getId())
        );
      } catch (ProcessingException e){
        // make sure that the indexing of the object is not interrupted by a failed post of the solr xml
        String msg = String.format("Failed indexing facets datastream for digital object %s. Root cause: %s", digitalObject.getId(), e);
        facetsDatastreamReports.add(
                new IntegrationActionReport(projectAbbr, IntegrationActionType.INDEX_OBJECT, IntegrationActionStatus.ERROR, msg)
        );
      }

    });

    try {
      client.commit();
      String msg = String.format("Successfully committed SOLR indexing operation for project %s", projectAbbr);
      log.info(msg);
      return facetsDatastreamReports;
    } catch (SolrServerException | IOException e) {
      String msg = String.format("Failed to commit SOLR indexing operation for project %s . Original error message: %s", projectAbbr, e);
      log.error(msg);
      throw new ProcessingException(msg);
    }
  }

  @Override
  public IntegrationActionReport deleteIndexedObjects(String projectAbbr) {
    log.trace("*** Trying to delete solr indexed project objects for : {}", projectAbbr);

    SolrClient client = getSolrClient();
    String solrDeletionQuery = String.format("%s:%s", BaseSearchProperties.PROJECT.name, projectAbbr);
    try {
      client.deleteByQuery(solrDeletionQuery);
      client.commit();
      String msg = String.format("Committed SOLR delete all indexing operation for project %s via built solr-query %s", projectAbbr, solrDeletionQuery);
      log.info(msg);
      return new IntegrationActionReport(projectAbbr, IntegrationActionType.DELETE_OBJECT, IntegrationActionStatus.SUCCESS, msg);
    } catch (SolrServerException | IOException e){
      String msg = String.format("Failed to delete all solr documents for project %s", projectAbbr);
      log.error(msg);
      throw new ProcessingException(msg);
    }
  }

  @Override
  public List<IntegrationActionReport> indexObject(String projectAbbr, String id) {
    SolrClient client = getSolrClient();
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
      postSolrDatastream(digitalObject);
      String msg = String.format("Successfully index facets datastream of digital object %s", digitalObject.getId());
      log.info(msg);
      indexingReports.add(new IntegrationActionReport(projectAbbr, IntegrationActionType.INDEX_OBJECT, IntegrationActionStatus.SUCCESS, msg));
    } catch (ProcessingException e){
      String msg = String.format("Failed to index facets datastream of digital object %s. Root cause: %s", digitalObject.getId(), e);
      log.error(msg);
      indexingReports.add(new IntegrationActionReport(projectAbbr, IntegrationActionType.INDEX_OBJECT, IntegrationActionStatus.ERROR, msg));
    }

    return indexingReports;

  }

  @Override
  public IntegrationActionReport deleteIndexedObject(String projectAbbr, String id) {
    SolrClient client = getSolrClient();
    String solrDeletionQuery = String.format("%s:%s", BaseSearchProperties.OBJECT_ID.name, id);
    try {
      client.deleteByQuery(solrDeletionQuery);
      client.commit();
      String msg = String.format("Committed SOLR delete object %s operation for project %s via built solr-query %s", id, projectAbbr, solrDeletionQuery);
      log.info(msg);
      return new IntegrationActionReport(projectAbbr, IntegrationActionType.DELETE_OBJECT, IntegrationActionStatus.SUCCESS, msg);
    } catch (SolrServerException | IOException e){
      String msg = String.format("Failed to delete all solr documents for digital object with id %s project %s", id, projectAbbr);
      log.error(msg);
      return new IntegrationActionReport(projectAbbr, IntegrationActionType.DELETE_OBJECT, IntegrationActionStatus.ERROR, msg);
    }
  }


  public SolrClient getSolrClient(){
    //final String solrUrl = "http://localhost:8983/solr/gams";
    final String solrUrl = configProperties.getFacetSearchUrl();
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
    solrInputDocument.addField(BaseSearchProperties.DATASTREAMS.name, digitalObject.getDatastreams().stream().map(Datastream::getDsid).collect(Collectors.toList()));
    solrInputDocument.addField(BaseSearchProperties.TYPE.name, BaseSearchTypes.DIGITAL_OBJECT.name);

    // index full text
    // TODO add missing validation (there must be a source_xml?)
    digitalObject.getDatastreams().stream().filter(datastream -> datastream.getDsid().equals(GAMSAPIntegrationDatastreamId.SOURCE_DATASTREAM_ID.name)).forEach(datastream -> {
      String fulltext = XMLUtils.extractText(XMLUtils.parseXml(datastream.getData()));
      solrInputDocument.addField(BaseSearchProperties.FULLTEXT.name, fulltext);
    });
    return solrInputDocument;
  }


  /**
   * Posts a custom solr datastream to the solr instance via a post request if available
   * and valid.
   * @param digitalObject digital object to be indexed
   */
  private void postSolrDatastream(DigitalObject digitalObject) throws ProcessingException {
    Optional<Datastream> datastreamOptional = digitalObject.getDatastreams().stream().filter(dstream -> dstream.getDsid().equals(GAMSAPIntegrationDatastreamId.SEARCH_DATASTREAM_ID.name)).findFirst();
    Datastream datastream;
    if(datastreamOptional.isEmpty()) {
      // if no search.json - skip processing
      String msg = String.format("Failed / Skipped  indexing custom facets datastream because none found at digital object %s", digitalObject.getId());
      log.error(msg);
      throw new ProcessingException(msg);
    } else {
      datastream = datastreamOptional.get();
    }

    BaseSearch[] facets;
    ObjectMapper objectMapper = new ObjectMapper();

    try {
      facets = objectMapper.readValue(datastream.getData(), BaseSearch[].class);
    } catch (IOException e){
      String msg = String.format("Failed to parse custom solr datastream to solr. Digital object: %s Cause: %s Original error message: %s", digitalObject.getId(), e.getMessage(), e);
      log.error(msg);
      throw new ProcessingException(msg);
    }

    // ensures that each solr entity = document has gams-controlled properties assigned
    Arrays.stream(facets).forEach(facet -> {
      facet.properties.put(BaseSearchProperties.OBJECT_ID.name, digitalObject.getId());
      facet.properties.put(BaseSearchProperties.PROJECT.name, digitalObject.getProject().getProjectAbbr());
      facet.properties.put(BaseSearchProperties.TYPE.name, BaseSearchTypes.DERIVATIVE.name);
      // id must be defined outside
    });

    String builtJson = "";
    try {
      builtJson = objectMapper.writeValueAsString(facets);
    } catch (JsonProcessingException e){
      String msg = String.format("Failed to marshal Facet objects to json array. Skipping solr indexing. Digital object %s . Cause: %s Original error message: %s", digitalObject.getId(), e.getMessage(), e);
      log.error(msg);
      throw new ProcessingException(msg);
    }

    log.info("Built json: {}", builtJson);

    // TODO need to block sending of json if a add document operation.
    // TODO this json needs some kind of validation e.g. every doc must have a projectAbbreviation assigned etc.

    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>(builtJson, httpHeaders);

    //TODO improve handling of RestClientException?
    ResponseEntity<String> response;
    try {
      String postUrl = String.format("%s/update/json/docs?commit=true", configProperties.getFacetSearchUrl());
      response = restTemplate.postForEntity(postUrl, request, String.class);
    } catch (RestClientException e){
      String msg = String.format("Failed to post custom solr datastream to solr instance. Digital object: %s Cause: %s Original error message: %s", digitalObject.getId(), e.getMessage(), e);
      log.error(msg);
      throw new ProcessingException(msg);
    }

    if(response.getStatusCode().isError()){
      String msg = String.format("Failed to post custom solr datastream to solr instance for object %s Response status code: %s", digitalObject.getId(), response.getStatusCode());
      log.error(msg);
      throw new ProcessingException(msg);
    } else {
      log.trace("Successfully posted custom solr xml datastream for object {} to  solr instance. Response status code: {}", digitalObject.getId(), response.getStatusCode());
    }

  }

}
