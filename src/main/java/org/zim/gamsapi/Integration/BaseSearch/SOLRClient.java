package org.zim.gamsapi.Integration.BaseSearch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
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
   * Check if a core exists for a given project.
   * Requests against the select endpoint of the core. (If an http error is sent back -> core doesn't exist)
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
