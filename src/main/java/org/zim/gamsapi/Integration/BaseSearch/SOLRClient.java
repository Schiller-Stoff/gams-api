package org.zim.gamsapi.Integration.BaseSearch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.zim.gamsapi.System.configproperties.GAMSDockerDNS;
import reactor.core.publisher.Mono;

/**
 * Client for interacting with the SOLR server.
 *
 */
@Slf4j
@Component
public class SOLRClient {

  private final GAMSDockerDNS configProperties;
  private final WebClient webClient;

  public SOLRClient(GAMSDockerDNS configProperties) {
    this.configProperties = configProperties;

    this.webClient = WebClient.builder()
        .baseUrl(configProperties.getBaseSearchUrl())
        .build();

  }

  // TODO add create core method


  /**
   * Check if a core exists for a given project.
   * Requests against the select endpoint of the core. (If an http error is sent back -> core doesn't exist)
   * @param coreName the name of the core to check
   * @return true if the core exists, false otherwise
   */
  public boolean coreExists(String coreName){
    log.debug("Checking if core {} exists", coreName);
    String coreStatusUrl = configProperties.getBaseSearchUrl() + "/" + coreName + "/select";

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





//    // proceed only if the core doesn't exist
//    try {
//      var responseEntity = restTemplate.exchange(coreStatusUrl, HttpMethod.GET, null, String.class);
//      // abort if core already exists
//      if(responseEntity.getStatusCode().equals(HttpStatus.OK)){
//        String msg = String.format("A solr core already exists for the project %s", projectAbbr);
//        log.debug(msg);
//        return true;
//      }
//    } catch (HttpClientErrorException e){
//      return false;
//    }
//
//    return false;
  }


}
