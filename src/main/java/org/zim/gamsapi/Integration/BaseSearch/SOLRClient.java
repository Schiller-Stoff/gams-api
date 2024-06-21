package org.zim.gamsapi.Integration.BaseSearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.zim.gamsapi.Integration.Common.exceptions.IntegrationServiceException;
import org.zim.gamsapi.Integration.Common.exceptions.ProcessingException;
import org.zim.gamsapi.System.configproperties.GAMSDockerDNS;
import reactor.core.publisher.Mono;

/**
 * Client for interacting with the SOLR server.
 */
@Slf4j
@Component
public class SOLRClient {
  private final WebClient webClient;

  private final String SOLR_CORE_API_ENDPOINT = "/api/cores";

  private final String SOLR_SINGLE_CORE_API_ENDPOINT = "/solr";

  private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    log.trace("Posting base search entity to solr");

    byte[] json;
    try {
      json = this.OBJECT_MAPPER.writeValueAsBytes(baseSearchEntities);
      log.trace("Mapped base search entities to json: {}", json);
    } catch (Exception e) {
      String msg = String.format("Failed to convert base search entity to json. Cause: %s. Original error: %s", e.getMessage(), e);
      log.error(msg);
      throw new ProcessingException(msg);
    }

    this.post(coreName, json);

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
        .retrieve();
  }

  /**
   * Creates a new core on the SOLR server.
   * @param coreName the name of the core to create
   * @return the response body from the server
   */
  public String createCore(String coreName) {
    final String URL = SOLR_CORE_API_ENDPOINT;
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
          .uri(URL)
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(body))
          .retrieve()
          .bodyToMono(String.class)
          .block();
    } catch (WebClientException e) {
      String msg = String.format("Failed to create solr core for project %s. With url %s and body %s Cause: %s. Original error: %s", coreName, URL, body, e.getMessage(), e);
      log.error(msg);
      throw new IntegrationServiceException(msg);
    }
  }

  /**
   * Deletes all documents in a core that match a query.
   * @param coreName the name of the core to delete documents from
   * @param query the query to match documents to delete
   */
  public void delete(String coreName, String query){

    final String URL = SOLR_SINGLE_CORE_API_ENDPOINT + "/" + coreName + "/update?commit=true";

    String body = """
                {
                    "delete": {
                      "query": "%s"
                    }
                  }
            """.formatted(query);

    log.trace("Deleting documents from core {} with query {} at base url {} and constructed body {}", coreName, query, URL, body);

    webClient.post()
        .uri(URL)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(body))
        .retrieve();
  }


  /**
   * Check if a core exists for a given project.
   * Requests against the select endpoint of the core. (If a http error is sent back -> core doesn't exist)
   * @param coreName the name of the core to check
   * @return true if the core exists, false otherwise
   */
  public boolean coreExists(String coreName){
    log.trace("Checking if core {} exists", coreName);
    final String CORE_STATUS_URL = SOLR_SINGLE_CORE_API_ENDPOINT + "/" + coreName + "/select";

    try {
      return Boolean.TRUE.equals(
          webClient.get()
            .uri(CORE_STATUS_URL)
            .retrieve()
            .onStatus(HttpStatusCode::isError, clientResponse -> Mono.error(new HttpClientErrorException(clientResponse.statusCode())))
            .bodyToMono(String.class)
            .map(response -> true)
            .onErrorReturn(false)
            .block()
      );
    } catch (WebClientException e){
      String msg = String.format("Failed to check if solr core exists for project %s. Via url: %s Cause: %s Original error: %s", coreName, CORE_STATUS_URL, e.getMessage(), e);
      log.error(msg);
      throw new IntegrationServiceException(msg);
    }

  }
}
