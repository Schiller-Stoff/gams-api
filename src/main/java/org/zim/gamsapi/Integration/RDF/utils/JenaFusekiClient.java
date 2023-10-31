package org.zim.gamsapi.Integration.RDF.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.Integration.Common.exceptions.ProcessingException;
import org.zim.gamsapi.System.configproperties.GAMSDockerDNS;


/**
 * Client encapsulating basic functionalities for handling jena - fuseki
 * e.g. sending data or handling rdf.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JenaFusekiClient {

  private final RestTemplate restTemplate = new RestTemplate();
  private final GAMSDockerDNS configProperties;

  /**
   * Posts given turtle to jena-fuseki.
   * @param digitalObject needed for context information in logging.
   * @param turtle turtle as string to be posted.
   */
  public void postNQuads(DigitalObject digitalObject, String turtle) throws ProcessingException {

    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(HttpHeaders.CONTENT_TYPE, RDFHttpContentTypes.TEXT_N_QUADS.name);
    HttpEntity<String> request = new HttpEntity<>(turtle, httpHeaders);

    ResponseEntity<String> response;
    try {
      String postUrl = configProperties.getTriplestoreUrl();
      response = restTemplate.postForEntity(postUrl, request, String.class);
    } catch (RestClientException e){
      String msg = String.format("Failed to post custom RDF datastream to triplestore instance. Digital object: %s. Cause: %s Original error message: %s", digitalObject.getId(), e.getMessage(), e);
      log.error(msg);
      throw new ProcessingException(msg);
    }

    if(response.getStatusCode().isError()){
      String msg = String.format("Failed to post custom rdf datastream to triplestore instance for object %s Response status code: %s", digitalObject.getId(), response.getStatusCode());
      log.error(msg);
      throw new ProcessingException(msg);
    } else {
      log.trace("Successfully posted custom rdf datastream for object {} to  fuseki instance. Response status code: {}", digitalObject.getId(), response.getStatusCode());
    }

  }

  /**
   * Sends given updated SPARQL to jena-fuseki.
   * @param context context of the operation - e.g. project abbreviation or id.
   * @param sparql sparql to be performed.
   */
  public void postSPARQL(String context, String sparql){
    HttpHeaders httpHeaders = new HttpHeaders();
    // reminder: SPARQL update needs to be done via formdata request.
    httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<String, String>();
    formData.add("update", sparql);
    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, httpHeaders);

    ResponseEntity<String> response;
    try {
      String postUrl = configProperties.getTriplestoreUrl();
      response = restTemplate.postForEntity(postUrl, request, String.class);
    } catch (RestClientException e){
      String msg = String.format("Failed to post SPARQL to triplestore instance. Context: %s. Cause: %s Original error message: %s", context, e.getMessage(), e);
      log.error(msg);
      throw new ProcessingException(msg);
    }

    if(response.getStatusCode().isError()){
      String msg = String.format("Failed to post custom rdf datastream to triplestore instance in context %s Response status code: %s", context, response.getStatusCode());
      log.error(msg);
      throw new ProcessingException(msg);
    } else {
      log.trace("Successfully posted custom rdf datastream in context {} to  fuseki instance. Response status code: {}", context, response.getStatusCode());
    }

  }

  /**
   * Constructs a triple representing the default metadata of a digital object.
   * @param digitalObject Digital object to represent.
   * @return Turtle of digital object.
   */
  public String buildDefaultIndexingTriple(DigitalObject digitalObject){
    StringBuilder turtle = new StringBuilder();
    turtle.append(
            String.format("<SERVER_REPLACEMENT/%s> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <SERVER_REPLACEMENT/ontology#digitalObject> <SERVER_REPLACEMENT/%s>.", digitalObject.getId(), digitalObject.getId())
    );
    turtle.append(String.format("<SERVER_REPLACEMENT/%s> %s \"%s\" <SERVER_REPLACEMENT/%s>.",digitalObject.getId(), RDFSearchProperties.HAS_ID.name, digitalObject.getId(), digitalObject.getId()));

    turtle.append(String.format("<SERVER_REPLACEMENT/%s> %s \"%s\" <SERVER_REPLACEMENT/%s>.",digitalObject.getId(), RDFSearchProperties.HAS_PROJECT_ABBR.name, digitalObject.getProject().getProjectAbbr(), digitalObject.getId()));

    digitalObject.getDatastreams().forEach(datastream -> {
      turtle.append(String.format("<SERVER_REPLACEMENT/%s> %s \"%s\" <SERVER_REPLACEMENT/%s>.",digitalObject.getId(), RDFSearchProperties.HAS_DATASTREAM.name, datastream.getDsid(), digitalObject.getId()));
    });

    // replace server names to the defined via enums.
    return turtle.toString().replaceAll("SERVER_REPLACEMENT", RDFSearchProperties.GAMS_BASE_URL.name);
  }

}
