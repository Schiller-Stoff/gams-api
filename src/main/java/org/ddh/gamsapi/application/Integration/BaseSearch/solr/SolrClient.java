package org.ddh.gamsapi.application.Integration.BaseSearch.solr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearch;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearchProperties;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationDataProcessingException;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationServiceException;
import org.ddh.gamsapi.infrastructure.System.configproperties.GAMSDockerDNS;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Client for interacting with the SOLR server.
 */
@Slf4j
@Component
public class SolrClient {
  private final WebClient webClient;

  private final String SOLR_CORE_API_ENDPOINT = "/api/cores";

  private final String SOLR_SINGLE_CORE_API_ENDPOINT = "/solr";

  private final String SOLR_CORE_ADMIN_API_ENDPOINT = SOLR_SINGLE_CORE_API_ENDPOINT + "/admin/cores";

  private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final String SOLR_BASE_URL;

  public SolrClient(GAMSDockerDNS configProperties) {
    // TODO consider timeouts / retries / error handling / etc. against SOLR.
    SOLR_BASE_URL = configProperties.getBaseSearchUrl();
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
      throw new IntegrationDataProcessingException(msg);
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

    try {
      webClient.post()
          .uri(postUrl)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(data)
          .retrieve()
          .toBodilessEntity()
          .block();
    } catch (WebClientResponseException e) {
      // This exception contains the response body from the server
      String errorResponseBody = e.getResponseBodyAsString();
      String msg = String.format("Failed to post data to solr core %s. Via baseUrl %s and endpoint %s. Status: %s. Error response from solr: %s",
          coreName, SOLR_BASE_URL, postUrl, e.getStatusCode(), errorResponseBody);
      log.error(msg);
      throw new IntegrationServiceException(msg);
    } catch (WebClientException e) {
      String msg = String.format("Failed to post data to solr core %s. Via baseUrl %s and endpoint %s and body %s Cause: %s. Original error: %s", coreName, SOLR_BASE_URL, postUrl, Arrays.toString(data), e.getMessage(), e);
      log.error(msg);
      throw new IntegrationServiceException(msg);
    }


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
    } catch (WebClientResponseException e){
      // This exception contains the response body from the server
      String errorResponseBody = e.getResponseBodyAsString();
      String msg = String.format("Failed to create solr core for project %s. Via baseUrl %s and endpoint %s. Status: %s. Error response from solr: %s",
          coreName, SOLR_BASE_URL, URL, e.getStatusCode(), errorResponseBody);
      log.error(msg);
      throw new IntegrationServiceException(msg);
    } catch (WebClientException e) {
      String msg = String.format("Failed to create solr core for project %s. Via baseUrl %s and endpoint %s and body %s Cause: %s. Original error: %s", coreName, SOLR_BASE_URL, URL, body, e.getMessage(), e);
      log.error(msg);
      throw new IntegrationServiceException(msg);
    }
  }

  /**
   * Deletes all documents in a core that match a query.
   * @param coreName the name of the core to delete documents from
   * @param query the query to match documents to delete
   */
  public void delete(String coreName, String query) {

    final String URL = SOLR_SINGLE_CORE_API_ENDPOINT + "/" + coreName + "/update?commit=true";

    String body = """
            {
                "delete": {
                  "query": "%s"
                }
              }
        """.formatted(query);

    log.trace("Deleting documents from core {} with query {} at base url {} and constructed body {}", coreName, query, URL, body);

    try {
      webClient.post()
          .uri(URL)
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(body))
          .retrieve()
          .toBodilessEntity()
          .block();
    } catch (WebClientResponseException e) {
      // This exception contains the response body from the server
      String errorResponseBody = e.getResponseBodyAsString();
      String msg = String.format("Failed to delete documents from solr core %s. Via baseUrl %s and endpoint %s. Status: %s. Error response from solr: %s",
          coreName, SOLR_BASE_URL, URL, e.getStatusCode(), errorResponseBody);
      log.error(msg);
      throw new IntegrationServiceException(msg);
    } catch (WebClientException e) {
      String msg = String.format("Failed to delete documents from solr core %s. Via baseUrl %s and endpoint %s and body %s Cause: %s. Original error: %s", coreName, SOLR_BASE_URL, URL, body, e.getMessage(), e);
      log.error(msg);
      throw new IntegrationServiceException(msg);

    }

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

  /**
   * Deletes a core from the SOLR server.
   * @param coreName the name of the core to delete
   */
  public void removeCore(String coreName){

    final String URL = String.format("%s?action=UNLOAD&core=%s&deleteInstanceDir=true", SOLR_CORE_ADMIN_API_ENDPOINT, coreName);

    try {
      webClient.post()
          .uri(URL)
          .contentType(MediaType.APPLICATION_JSON)
          .retrieve()
          .bodyToMono(String.class)
          .block();
    } catch (WebClientResponseException e){
      // This exception contains the response body from the server
      String errorResponseBody = e.getResponseBodyAsString();
      String msg = String.format("Failed to delete solr core %s. Via baseUrl %s and endpoint %s. Status: %s. Error response from solr: %s",
          coreName, SOLR_BASE_URL, URL, e.getStatusCode(), errorResponseBody);
      log.error(msg);
      throw new IntegrationServiceException(msg);
    } catch (WebClientException e) {
      String msg = String.format("Failed to delete solr core %s. Via baseUrl %s and endpoint %s Cause: %s. Original error: %s", coreName, SOLR_BASE_URL, URL, e.getMessage(), e);
      log.error(msg);
      throw new IntegrationServiceException(msg);
    }


  }

  /**
   * Wipes all documents from a core.
   * @param coreName the name of the core to wipe
   */
  public void wipeCore(String coreName){
    log.trace("Wiping core {}", coreName);
    this.delete(coreName, "*:*");
  }

  /**
   * Checks if a core is empty.
   * @param coreName the name of the core to check
   */
  public boolean checkCoreIsEmpty(String coreName){
    log.trace("Checking if core {} is empty", coreName);
    final String CORE_QUERY_URL = SOLR_SINGLE_CORE_API_ENDPOINT + "/" + coreName + "/select?q=*:*&rows=0";

    try {
      String response = webClient.get()
          .uri(CORE_QUERY_URL)
          .retrieve()
          .bodyToMono(String.class)
          .block();

      // Parse the response to check numFound field
      int numFound = OBJECT_MAPPER.readTree(response)
          .path("response")
          .path("numFound")
          .asInt();

      log.trace("Core {} has {} documents", coreName, numFound);
      return numFound == 0;
    } catch (WebClientResponseException e) {
      String errorResponseBody = e.getResponseBodyAsString();
      String msg = String.format("Failed to check if solr core %s is empty. Via url: %s Status: %s. Error response from solr: %s",
          coreName, CORE_QUERY_URL, e.getStatusCode(), errorResponseBody);
      log.error(msg);
      throw new IntegrationServiceException(msg);
    } catch (Exception e) {
      String msg = String.format("Failed to check if solr core %s is empty. Via url: %s Cause: %s Original error: %s",
          coreName, CORE_QUERY_URL, e.getMessage(), e);
      log.error(msg);
      throw new IntegrationServiceException(msg);
    }

  }

  /**
   * Retrieve a document from a core by a specific property.
   * @param coreName name of the solr core
   * @param propertyName name of the property to search by (of the solr document)
   * @param propertyValue value of the property to search by
   * @return the response body from the server
   */
  public String retrieveSolrDocumentByProperty(String coreName, String propertyName, String propertyValue){

    final String CORE_QUERY_URL = String.format("%s/%s/select?q=%s:%s", SOLR_SINGLE_CORE_API_ENDPOINT, coreName, propertyName, propertyValue);
    log.trace("Retrieving document from core {} with property {}={}", coreName, propertyName, propertyValue);

    try {
      return webClient.get()
          .uri(CORE_QUERY_URL)
          .retrieve()
          .bodyToMono(String.class)
          .block();
    } catch (WebClientResponseException e) {
      String errorResponseBody = e.getResponseBodyAsString();
      String msg = String.format("Failed to retrieve document from solr core %s with property %s=%s. Via url: %s Status: %s. Error response from solr: %s",
          coreName, propertyName, propertyValue, CORE_QUERY_URL, e.getStatusCode(), errorResponseBody);
      log.error(msg);
      throw new IntegrationServiceException(msg);
    } catch (Exception e) {
      String msg = String.format("Failed to retrieve document from solr core %s with property %s=%s. Via url: %s Cause: %s Original error: %s",
          coreName, propertyName, propertyValue, CORE_QUERY_URL, e.getMessage(), e);
      log.error(msg);
      throw new IntegrationServiceException(msg);
    }

  }


  /**
   * Execute a Solr query and return the raw JSON response.
   *
   * @param solrQuery Argument after 'q=' in Solr query URL
   * @return Raw JSON response from Solr
   */
  public String query(String coreName, String solrQuery) {
    final String CORE_QUERY_URL = String.format("%s/%s/select?q=%s", SOLR_SINGLE_CORE_API_ENDPOINT, coreName, solrQuery);
    log.trace("Executing Solr query: {}", CORE_QUERY_URL);

    try {
      return webClient.get()
          .uri(CORE_QUERY_URL)
          .retrieve()
          .bodyToMono(String.class)
          .block();
    } catch (WebClientResponseException e) {
      String errorResponseBody = e.getResponseBodyAsString();
      String msg = String.format(
          "Failed to execute Solr query. SOLR-URL: %s, Status: %s, Error: %s",
          CORE_QUERY_URL, e.getStatusCode(), errorResponseBody
      );
      log.error(msg);
      throw new IntegrationServiceException(msg);
    } catch (WebClientException e) {
      String msg = String.format(
          "Failed to execute Solr query. SOLR-URL: %s, Cause: %s",
          CORE_QUERY_URL, e.getMessage()
      );
      log.error(msg);
      throw new IntegrationServiceException(msg);
    }
  }

  /**
   * Count documents in a Solr core for a specific project.
   * @param coreName
   * @param projectAbbrs
   * @return
   */
  public int countProjectDocuments(String coreName, Set<String> projectAbbrs){

    StringBuilder url = new StringBuilder();
    url.append(String.format("/solr/%s/select?", coreName));

    // Project filter
    if (projectAbbrs.size() == 1) {
      url.append(String.format("q=%s:%s",
          BaseSearchProperties.PROJECT.name,
          SolrUrlBuilder.escapeSolrValue(projectAbbrs.iterator().next())));
    } else {
      String projectQuery = projectAbbrs.stream()
          .map(abbr -> String.format("%s:%s",
              BaseSearchProperties.PROJECT.name,
              SolrUrlBuilder.escapeSolrValue(abbr)))
          .collect(Collectors.joining(" OR "));
      url.append("&q=(").append(projectQuery).append(")");
    }

    // We only need the count
    url.append("&rows=0");
    url.append("&wt=json");
    url.append("&indent=true");


    log.info("Counting documents in Solr core {} for projects {} with URL: {}", coreName, projectAbbrs, url);

    String solrResponse;
    try {
      solrResponse = webClient.get()
          .uri(url.toString())
          .retrieve()
          .bodyToMono(String.class)
          .block();

    } catch (WebClientResponseException e) {
      String errorResponseBody = e.getResponseBodyAsString();
      String msg = String.format(
          "Failed to count documents in Solr core %s for projects %s. SOLR-URL: %s, Status: %s, Error: %s",
          coreName, projectAbbrs, url, e.getStatusCode(), errorResponseBody
      );
      log.error(msg);
      throw new IntegrationServiceException(msg);
    } catch (WebClientException e) {
      String msg = String.format(
          "Failed to count documents in Solr core %s for projects %s. SOLR-URL: %s, Cause: %s",
          coreName, projectAbbrs, url, e.getMessage()
      );
      log.error(msg);
      throw new IntegrationServiceException(msg);
    }

    try {
      // Parse the response to extract numFound
      int numFound = OBJECT_MAPPER.readTree(solrResponse)
          .path("response")
          .path("numFound")
          .asInt();

      return numFound;
    } catch (Exception e) {
      String msg = String.format(
          "Failed to parse Solr count response for core %s and projects %s. Cause: %s",
          coreName, projectAbbrs, e.getMessage()
      );
      log.error(msg);
      throw new IntegrationDataProcessingException(msg);
    }

  }

  /**
   * Execute a Solr query and return the raw JSON response.
   * Handles complex Solr query syntax including special characters like {}, !, etc.
   *
   * @param url The complete query path including parameters (e.g., "/solr/gams/select?q=*:*")
   * @return Raw JSON response from Solr
   */
  public String get(String url) {
    log.trace("Executing Solr url: {}", url);

    try {
      // using uri to avoid encoding issues with special characters
      URI uri = URI.create(SOLR_BASE_URL + url);

      return webClient.get()
          .uri(uri)  // Use URI object instead of String
          .retrieve()
          .bodyToMono(String.class)
          .block();
    } catch (WebClientResponseException e) {
      String errorResponseBody = e.getResponseBodyAsString();
      assert e.getRequest() != null;
      String msg = String.format(
          "Failed to execute Solr query via url %s. Status: %s, Error: %s",
          e.getRequest().getURI(), e.getStatusCode(), errorResponseBody
      );
      log.error(msg);
      throw new IntegrationServiceException(msg);
    } catch (WebClientException e) {
      String msg = String.format(
          "Failed to execute Solr query. Path: %s, Cause: %s",
          url, e.getMessage()
      );
      log.error(msg);
      throw new IntegrationServiceException(msg);
    }
  }

}
