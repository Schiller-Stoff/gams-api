package org.zim.gamsapi.Integration.BaseSearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.server.ResponseStatusException;
import org.zim.gamsapi.Integration.Common.exceptions.ProcessingException;
import org.zim.gamsapi.System.configproperties.GAMSDockerDNS;
import reactor.core.publisher.Mono;

/**
 * Client for interacting with the SOLR server.
 * TODO think about exceptions thrown by this client (and by the webclient underneath)
 */
@Slf4j
@Component
public class SOLRClient {
  private final WebClient webClient;

  private final String SOLR_CORE_API_ENDPOINT = "/api/cores";

  private final String SOLR_SINGLE_CORE_API_ENDPOINT = "/solr";

  public SOLRClient(GAMSDockerDNS configProperties) {
    // TODO consider timeouts / retries / error handling / etc. against SOLR.
    this.webClient = WebClient.builder()
        .baseUrl(configProperties.getBaseSearchUrl())
        .build();
  }

  /**
   * Post a base search entity to the solr server.
   * @param coreName name of the core to post to
   * @param baseSearchEntities the base search entities to post
   */
  public void post(String coreName, BaseSearch[] baseSearchEntities){
    log.debug("Posting base search entity to solr");

    // TODO method has various issues

    String json = "";
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      json = objectMapper.writeValueAsString(baseSearchEntities);
      log.trace("Mapped base search entities to json: {}", json);
    } catch (Exception e) {
      log.error("Error while converting base search entity to json string", e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while converting base search entity to json");
    }

    String postUrl = String.format("%s/%s/update/json/docs?commit=true", SOLR_SINGLE_CORE_API_ENDPOINT, coreName);

    webClient.post()
        .uri(postUrl)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(json)
        .retrieve()
        .toBodilessEntity()
        .toFuture();

    // alternative way to do the same thing (but not async)

//    var response = webClient.post()
//        .uri(postUrl)
//        .contentType(MediaType.APPLICATION_JSON)
//        .bodyValue(json)
//        .retrieve()
//        .bodyToMono(String.class);


//    try {
//      String response = responseMono.block();
//      String msg = String.format("Successfully posted custom search xml datastream for object to solr instance.");
//      log.trace(msg);
//      //return new IntegrationActionReport(projectAbbr, IntegrationActionType.INDEX_OBJECT, IntegrationActionStatus.SUCCESS, msg);
//    } catch (WebClientException e) {
//      String msg = String.format("Failed to post custom solr datastream to solr instance. Cause: %s Original error message: %s", e.getMessage(), e);
//      log.error(msg);
//      throw new ProcessingException(msg);
//    }


  }


  /**
   * Post a single base search entity to the solr server.
   * @param coreName name of the core to post to
   * @param baseSearchEntity the base search entity to post
   */
  public void post(String coreName, BaseSearch baseSearchEntity){
    post(coreName, new BaseSearch[]{baseSearchEntity});
  }

  public void post(String coreName, byte[] data){
    log.trace("Posting now byte array data to solr core {}", coreName);

    String postUrl = String.format("%s/%s/update/json/docs?commit=true", SOLR_SINGLE_CORE_API_ENDPOINT, coreName);

    webClient.post()
        .uri(postUrl)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(data)
        .retrieve()
        .toBodilessEntity()
        .toFuture();
  }

  /**
   * Create a new core in the SOLR server.
   * @param coreName the name of the core to create
   * @return the response body from the server
   */
  public String createCore(String coreName) {
    String body = String.format("""
                {
                    "create": {
                      "name": "%s",
                      "configSet": "base"
                    }
                  }
              """, coreName);

    try {
      return webClient.post()
          .uri(SOLR_CORE_API_ENDPOINT)
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(body))
          .retrieve()
          .bodyToMono(String.class)
          .block();
    } catch (WebClientException e) {
      log.error("Error while creating the solr core for project {}", coreName, e);
      throw e;
    }
  }

  /**
   * Deletes all documents in a core that match a query.
   * @param coreName the name of the core to delete documents from
   * @param query the query to match documents to delete
   */
  public void delete(String coreName, String query){

    String url = SOLR_SINGLE_CORE_API_ENDPOINT + "/" + coreName + "/update";

    String body = """
                {
                    "delete": {
                      "query": "%s"
                    }
                  }
            """.formatted(query);

    log.error("Deleting documents from core {} with query {} at base url {} and constructed body {}", coreName, query, url, body);

    try {
      webClient.post()
          .uri(SOLR_SINGLE_CORE_API_ENDPOINT + "/" + coreName + "/update?commit=true")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(body))
          .retrieve()
          .toBodilessEntity()
          .toFuture()
          .get();
    } catch (Exception e) {
      String msg =  String.format("Error while deleting the solr core for project %s. Original error: %s", coreName, e);
      log.error(msg);
      throw new ProcessingException(msg);
    }

  }


  /**
   * Check if a core exists for a given project.
   * Requests against the select endpoint of the core. (If a http error is sent back -> core doesn't exist)
   * @param coreName the name of the core to check
   * @return true if the core exists, false otherwise
   */
  public boolean coreExists(String coreName){
    log.debug("Checking if core {} exists", coreName);
    String coreStatusUrl = SOLR_SINGLE_CORE_API_ENDPOINT + "/" + coreName + "/select";

    try {
      return Boolean.TRUE.equals(
          webClient.get()
            .uri(coreStatusUrl)
            .retrieve()
            .onStatus(HttpStatusCode::isError, clientResponse -> Mono.error(new HttpClientErrorException(clientResponse.statusCode())))
            .bodyToMono(String.class)
            .map(response -> true)
            .onErrorReturn(false)
            .block()
      );
    } catch (WebClientException e){
      log.error("Error while checking if the solr core exists for project {}", coreName, e);
      throw e;
    }

  }
}
