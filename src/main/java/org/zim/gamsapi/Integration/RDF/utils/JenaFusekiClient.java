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
import org.zim.gamsapi.Integration.ProcessingException;
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
  public void postNQuads(DigitalObject digitalObject, String turtle) {

    HttpHeaders httpHeaders = new HttpHeaders();
    // TODO reminder: fuseki needs special content-types, like text/turtle or json+ld to handle triple processing
    // standard content-types like text/plain will not work
    // atm only text/turtle allowed!
    // TODO enum for content-type?
    httpHeaders.add("content-type", "text/n-quads");
    HttpEntity<String> request = new HttpEntity<>(turtle, httpHeaders);

    ResponseEntity<String> response;
    try {
      String postUrl = configProperties.getTriplestoreUrl();
      response = restTemplate.postForEntity(postUrl, request, String.class);
    } catch (RestClientException e){
      String msg = String.format("Failed to post custom RDF datastream to triplestore instance. Digital object: %s. Cause: %s Original error message: %s", digitalObject.getPid(), e.getMessage(), e);
      log.error(msg);
      throw new ProcessingException(msg);
    }

    if(response.getStatusCode().isError()){
      String msg = String.format("Failed to post custom rdf datastream to triplestore instance for object %s Response status code: %s", digitalObject.getPid(), response.getStatusCode());
      log.error(msg);
      throw new ProcessingException(msg);
    } else {
      log.trace("Successfully posted custom rdf datastream for object {} to  fuseki instance. Response status code: {}", digitalObject.getPid(), response.getStatusCode());
    }

  }

  /**
   * Sends given updated SPARQL to jena-fuseki.
   * @param context context of the operation - e.g. project abbreviation or pid.
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
      // TODO refactor logging message
      String msg = String.format("Failed to post SPARQL to triplestore instance. Context: %s. Cause: %s Original error message: %s", context, e.getMessage(), e);
      log.error(msg);
      throw new ProcessingException(msg);
    }

    if(response.getStatusCode().isError()){
      // TODO refactor logging message
      String msg = String.format("Failed to post custom rdf datastream to triplestore instance for object %s Response status code: %s", context, response.getStatusCode());
      log.error(msg);
      throw new ProcessingException(msg);
    } else {
      log.trace("Successfully posted custom rdf datastream for object {} to  fuseki instance. Response status code: {}", context, response.getStatusCode());
    }

  }

  /**
   * Constructs a triple representing the default metadata of a digital object.
   * @param digitalObject Digital object to represent.
   * @return Turtle of digital object.
   */
  public String buildDefaultIndexingTriple(DigitalObject digitalObject){

    //TODO refactor building of indexing triple via using the jena rdf-api

    StringBuilder turtle = new StringBuilder();
    turtle.append(
            String.format("<https://gams.uni-graz.at/%s> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://gams.uni-graz.at/ontology#digitalObject> <https://gams.uni-graz.at/%s>.", digitalObject.getPid(), digitalObject.getPid())
    );
    turtle.append(String.format("<https://gams.uni-graz.at/%s> %s \"%s\" <https://gams.uni-graz.at/%s>.",digitalObject.getPid(), RDFSearchProperties.HAS_PID.name, digitalObject.getPid(), digitalObject.getPid()));

    turtle.append(String.format("<https://gams.uni-graz.at/%s> %s \"%s\" <https://gams.uni-graz.at/%s>.",digitalObject.getPid(), RDFSearchProperties.HAS_PROJECT_ABBR.name, digitalObject.getProjectAbbr(), digitalObject.getPid()));

    digitalObject.getDatastreams().forEach(datastream -> {
      turtle.append(String.format("<https://gams.uni-graz.at/%s> %s \"%s\" <https://gams.uni-graz.at/%s>.",digitalObject.getPid(), RDFSearchProperties.HAS_DATASTREAM.name, datastream.getDsid(), digitalObject.getPid()));
    });

    return turtle.toString();
  }

}
