package org.ddh.gamsapi.application.Integration.Common.utils.solr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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

  private final SolrClientProperties solrClientProperties;

  public SolrClient(GAMSDockerDNS configProperties, SolrClientProperties solrClientProperties) {
    // TODO consider timeouts / retries / error handling / etc. against SOLR.
    SOLR_BASE_URL = configProperties.getBaseSearchUrl();
    this.solrClientProperties = solrClientProperties;
    this.webClient = WebClient.builder()
        .baseUrl(configProperties.getBaseSearchUrl())
        .build();

    log.info("SolrClient initialized with autoCommit={}, batchSize={}, commitInterval={}",
        solrClientProperties.isAutoCommit(), solrClientProperties.getBatchSize(), solrClientProperties.getCommitInterval());
  }

  /**
   * Post a base search entity to the solr server.
   *
   * @param coreName           name of the core to post to
   * @param solrDocuments the base search entities to post
   */
  public void post(String coreName, SolrDocument[] solrDocuments, boolean commit) {
    log.trace("Posting base search entity to solr");

    byte[] json;
    try {
      json = this.OBJECT_MAPPER.writeValueAsBytes(solrDocuments);
      log.trace("Mapped base search entities to json: {}", json);
    } catch (Exception e) {
      String msg = "Failed to convert solr document to json. Cause: " + e.getMessage();
      log.error(msg);
      throw new IntegrationDataProcessingException(
          msg,
          e
      );
    }

    this.post(coreName, json, commit);

  }

  /**
   * Post a single base search entity to the solr server.
   *
   * @param coreName         name of the core to post to
   * @param solrDocument the base search entity to post
   */
  public void post(String coreName, SolrDocument solrDocument) {
    post(coreName, new SolrDocument[]{solrDocument}, solrClientProperties.isAutoCommit());
  }

  /**
   * Post a single base search entity to the solr server.
   *
   * @param coreName         name of the core to post to
   * @param solrDocument the base search entity to post
   */
  public void post(String coreName, SolrDocument solrDocument, boolean commit) {
    post(coreName, new SolrDocument[]{solrDocument}, commit);
  }

  /**
   * Post multiple base search entities to the solr server.
   *
   * @param coreName           name of the core to post to
   * @param solrDocuments the base search entities to post
   */
  public void post(String coreName, SolrDocument[] solrDocuments) {
    post(coreName, solrDocuments, solrClientProperties.isAutoCommit());
  }

  /**
   * Post raw JSON bytes to the solr server.
   *
   * @param coreName name of the core to post to
   * @param json     the JSON bytes to post
   */
  public void post(String coreName, byte[] json) {
    post(coreName, json, true);  // Default: commit
  }

  /**
   * Post raw JSON bytes to the solr server with optional commit.
   *
   * @param coreName name of the core to post to
   * @param json     the JSON bytes to post
   * @param commit   whether to commit after posting
   */
  public void post(String coreName, byte[] json, boolean commit) {
    log.trace("Posting byte array data to solr core {} (commit={})", coreName, commit);

    String postUrl = SOLR_SINGLE_CORE_API_ENDPOINT + "/" + coreName + "/update/json/docs?commit=" + commit;

    try {
      webClient.post()
          .uri(postUrl)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(json)
          .retrieve()
          .toBodilessEntity()
          .block();

      log.info("Successfully posted data to solr core {}", coreName);

    } catch (WebClientResponseException e) {
      String errorResponseBody = e.getResponseBodyAsString();
      String msg = "Failed to post data to solr core  " + coreName + ". Status: " + e.getStatusCode() +
          ". Error response from solr: " + errorResponseBody;
      log.error(msg);
      throw new IntegrationServiceException(
          msg,
          e
      );
    } catch (WebClientException e) {
      String msg = "Failed to post data to solr core " + coreName + ". Cause: " + e.getMessage();
      log.error(msg);
      throw new IntegrationServiceException(
          msg,
          e
      );
    }
  }

  /**
   * Creates a new core on the SOLR server.
   *
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
    } catch (WebClientResponseException e) {
      // This exception contains the response body from the server
      String errorResponseBody = e.getResponseBodyAsString();
      String msg = "Failed to create solr core " + coreName + ". Via baseUrl " + SOLR_BASE_URL +
          " and endpoint " + URL + ". Status: " + e.getStatusCode() +
          ". Error response from solr: " + errorResponseBody + " Original error: " + e.getMessage();
      log.error(msg);
      throw new IntegrationServiceException(
          msg,
          e
      );
    } catch (WebClientException e) {
      String msg = "Failed to create solr core " + coreName + ". Via baseUrl " + SOLR_BASE_URL +
          " and endpoint " + URL + " and body " + body +
          ". Cause: " + e.getMessage();
      log.error(msg);
      throw new IntegrationServiceException(
          msg,
          e
      );
    }
  }

  /**
   * Deletes all documents in a core that match a query.
   *
   * @param coreName the name of the core to delete documents from
   * @param query    the query to match documents to delete
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
      String msg = "Failed to delete documents from solr core " + coreName + ". Via baseUrl " + SOLR_BASE_URL +
          " and endpoint " + URL + ". Status: " + e.getStatusCode() +
          ". Error response from solr: " + errorResponseBody + " Original error: " + e.getMessage();
      log.error(msg);
      throw new IntegrationServiceException(msg,e);
    } catch (WebClientException e) {
      String msg = "Failed to delete documents from solr core " + coreName + ". Via baseUrl " + SOLR_BASE_URL +
          " and endpoint " + URL + " and body " + body +
          ". Cause: " + e.getMessage();
      log.error(msg);
      throw new IntegrationServiceException(msg, e);

    }

  }

  /**
   * Check if a core exists for a given project.
   * Requests against the select endpoint of the core. (If an http error is sent back -> core doesn't exist)
   *
   * @param coreName the name of the core to check
   * @return true if the core exists, false otherwise
   */
  public boolean coreExists(String coreName) {
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
    } catch (WebClientException e) {
      String msg = "Failed to check if solr core exists for project " + coreName + ". Via url: " + CORE_STATUS_URL +
          ". Cause: " + e.getMessage();
      log.error(msg);
      throw new IntegrationServiceException(msg, e);
    }

  }

  /**
   * Deletes a core from the SOLR server.
   *
   * @param coreName the name of the core to delete
   */
  public void removeCore(String coreName) {

    final String URL = SOLR_CORE_ADMIN_API_ENDPOINT + "?action=UNLOAD&core=" + coreName + "&deleteInstanceDir=true";

    try {
      webClient.post()
          .uri(URL)
          .contentType(MediaType.APPLICATION_JSON)
          .retrieve()
          .bodyToMono(String.class)
          .block();
    } catch (WebClientResponseException e) {
      // This exception contains the response body from the server
      String errorResponseBody = e.getResponseBodyAsString();
      String msg = "Failed to delete solr core " + coreName + ". Via baseUrl " + SOLR_BASE_URL +
          " and endpoint " + URL + ". Status: " + e.getStatusCode() +
          ". Error response from solr: " + errorResponseBody + " Original error: " + e.getMessage();
      log.error(msg);
      throw new IntegrationServiceException(msg, e);
    } catch (WebClientException e) {
      String msg = "Failed to delete solr core " + coreName + ". Via baseUrl " + SOLR_BASE_URL +
          " and endpoint " + URL +
          ". Cause: " + e.getMessage();
      log.error(msg);
      throw new IntegrationServiceException(msg, e);
    }


  }

  /**
   * Wipes all documents from a core.
   *
   * @param coreName the name of the core to wipe
   */
  public void wipeCore(String coreName) {
    log.trace("Wiping core {}", coreName);
    this.delete(coreName, "*:*");
  }

  /**
   * Checks if a core is empty.
   *
   * @param coreName the name of the core to check
   */
  public boolean checkCoreIsEmpty(String coreName) {
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
      String msg = "Failed to check if solr core " + coreName + " is empty. Via url: " + CORE_QUERY_URL +
          ". Status: " + e.getStatusCode() +
          ". Error response from solr: " + errorResponseBody + " Original error: " + e.getMessage();
      log.error(msg);
      throw new IntegrationServiceException(msg, e);
    } catch (Exception e) {
      String msg = "Failed to check if solr core " + coreName + " is empty. Via url: " + CORE_QUERY_URL +
          ". Cause: " + e.getMessage();
      log.error(msg, e);
      throw new IntegrationServiceException(msg, e);
    }

  }

  /**
   * Retrieve a document from a core by its ID.
   * @param coreName name of the solr core
   * @param documentId ID of the document to retrieve
   * @return the SolrDocument object
   */
  public SolrDocument retrieveSolrDocumentById(String coreName, String documentId) {
    String response = retrieveSolrDocumentByProperty(coreName, "id", documentId);
    try {
      JsonNode docsNode = OBJECT_MAPPER.readTree(response)
          .path("response")
          .path("docs");
      if (docsNode.isArray() && !docsNode.isEmpty()) {
        return OBJECT_MAPPER.treeToValue(docsNode.get(0), SolrDocument.class);
      } else {
        String msg = "Solr document with ID " + documentId + " not found in core " + coreName;
        log.debug(msg);
        // TODO different exception?
        throw new IntegrationDataProcessingException(msg);
      }
    } catch (Exception e) {
      String msg = "Failed to parse Solr document response for core " + coreName +
          " and document ID " + documentId + ". Cause: " + e.getMessage();
      log.error(msg, e);
      throw new IntegrationDataProcessingException(
          msg,
          e
      );
    }
  }

  /**
   * Retrieve a document from a core by a specific property.
   *
   * @param coreName      name of the solr core
   * @param propertyName  name of the property to search by (of the solr document)
   * @param propertyValue value of the property to search by
   * @return the response body from the server
   */
  public String retrieveSolrDocumentByProperty(String coreName, String propertyName, String propertyValue) {

    final String CORE_QUERY_URL = SOLR_SINGLE_CORE_API_ENDPOINT +  "/" + coreName + "/select?q=" + propertyName + ":" +  propertyValue;
    log.trace("Retrieving document from core {} with property {}={}", coreName, propertyName, propertyValue);

    try {
      return webClient.get()
          .uri(CORE_QUERY_URL)
          .retrieve()
          .bodyToMono(String.class)
          .block();
    } catch (WebClientResponseException e) {
      String errorResponseBody = e.getResponseBodyAsString();
      String msg = "Failed to retrieve document from solr core " + coreName + " with property " + propertyName + "=" + propertyValue +
          ". Via url: " + CORE_QUERY_URL + ". Status: " + e.getStatusCode() +
          ". Error response from solr: " + errorResponseBody + " Original error: " + e.getMessage();
      log.error(msg, e);
      throw new IntegrationServiceException(msg, e);
    } catch (Exception e) {
      String msg = "Failed to retrieve document from solr core " + coreName + " with property " + propertyName + "=" + propertyValue +
          ". Via url: " + CORE_QUERY_URL +
          ". Cause: " + e.getMessage();
      log.error(msg, e);
      throw new IntegrationServiceException(msg, e);
    }

  }


  /**
   * Execute a Solr query and return the raw JSON response.
   *
   * @param solrQuery Argument after 'q=' in Solr query URL
   * @return Raw JSON response from Solr
   */
  public String query(String coreName, String solrQuery) {
    final String CORE_QUERY_URL = SOLR_SINGLE_CORE_API_ENDPOINT + "/" + coreName + "/select?q=" + solrQuery;
    log.trace("Executing Solr query: {}", CORE_QUERY_URL);

    try {
      return webClient.get()
          .uri(CORE_QUERY_URL)
          .retrieve()
          .bodyToMono(String.class)
          .block();
    } catch (WebClientResponseException e) {
      String errorResponseBody = e.getResponseBodyAsString();
      String msg = "Failed to execute Solr query. SOLR-URL: " + CORE_QUERY_URL +
          ", Status: " + e.getStatusCode() +
          ", Error: " + errorResponseBody + " Original error: " + e.getMessage();
      log.error(msg, e);
      throw new IntegrationServiceException(msg, e);
    } catch (WebClientException e) {
      String msg = "Failed to execute Solr query. SOLR-URL: " + CORE_QUERY_URL +
          ", Cause: " + e.getMessage();
      log.error(msg, e);
      throw new IntegrationServiceException(msg, e);
    }
  }

  /**
   * Count documents in a Solr core by property values.
   *
   * @param coreName     name of the solr core
   * @param propertyName name of the property to be counted after
   * @param propertyValues set of values to count documents for (might be empty)
   * @return number of documents matching the property values
   */
  public int countDocumentsByPropertyValues(
      String coreName,
      String propertyName,
      Set<String> propertyValues) {

    StringBuilder url = new StringBuilder();
    url.append("/solr/")
        .append(coreName)
        .append("/select?");

    // Project filter
    if (propertyValues.isEmpty()) {
      url.append("q=")
          .append(propertyName)
          .append(":*");
    } else if (propertyValues.size() == 1) {
      url.append("q=")
          .append(propertyName)
          .append(":")
          .append(
              SolrUrlBuilder.escapeSolrValue(propertyValues.iterator().next())
          );
    } else {
      String projectQuery = propertyValues.stream()
          .map(abbr -> propertyName + ":" + SolrUrlBuilder.escapeSolrValue(abbr))
          .collect(Collectors.joining(" OR "));
      url.append("&q=(").append(projectQuery).append(")");
    }

    // We only need the count
    url.append("&rows=0");
    url.append("&wt=json");
    url.append("&indent=true");


    log.info("Counting documents in Solr core {} for projects {} with URL: {}", coreName, propertyValues, url);

    String solrResponse;
    try {
      solrResponse = webClient.get()
          .uri(url.toString())
          .retrieve()
          .bodyToMono(String.class)
          .block();

    } catch (WebClientResponseException e) {
      String errorResponseBody = e.getResponseBodyAsString();
      String msg = "Failed to count documents in Solr core " + coreName +
          " for projects " + propertyValues +
          ". SOLR-URL: " + url +
          ", Status: " + e.getStatusCode() +
          ", Error: " + errorResponseBody + " Original error: " + e.getMessage();
      log.error(msg, e);
      throw new IntegrationServiceException(msg, e);
    } catch (WebClientException e) {
      String msg = "Failed to count documents in Solr core " + coreName +
          " for projects " + propertyValues +
          ". SOLR-URL: " + url +
          ", Cause: " + e.getMessage();
      log.error(msg, e);
      throw new IntegrationServiceException(msg, e);
    }

    try {
      // Parse the response to extract numFound
      int numFound = OBJECT_MAPPER.readTree(solrResponse)
          .path("response")
          .path("numFound")
          .asInt();

      return numFound;
    } catch (Exception e) {
      String msg = "Failed to parse Solr count response for core " + coreName +
          " and projects " + propertyValues +
          ". Cause: " + e.getMessage();
      log.error(msg, e);
      throw new IntegrationDataProcessingException(msg, e);
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
      String msg = "Failed to execute Solr query via url " + url +
          ". Status: " + e.getStatusCode() +
          ", Error: " + errorResponseBody + " Original error: " + e.getMessage();
      log.error(msg, e);
      throw new IntegrationServiceException(msg, e);
    } catch (WebClientException e) {
      String msg = "Failed to execute Solr query via url " + url +
          ". Cause: " + e.getMessage();
      log.error(msg,e);
      throw new IntegrationServiceException(msg,e);
    }
  }


  /**
   * Explicitly commits changes to Solr core.
   * Use after batch indexing operations for optimal performance.
   *
   * @param coreName Solr core name
   */
  public void commit(String coreName) {
    log.debug("Committing core: {}", coreName);

    String commitUrl = SOLR_SINGLE_CORE_API_ENDPOINT + "/" + coreName + "/update?commit=true";

    try {
      webClient.post()
          .uri(commitUrl)
          .contentType(MediaType.APPLICATION_JSON)
          .retrieve()
          .toBodilessEntity()
          .block();

      log.info("Successfully committed core: {}", coreName);

    } catch (WebClientResponseException e) {
      String errorBody = e.getResponseBodyAsString();
      String msg = "Failed to commit core " + coreName + ". Status: " + e.getStatusCode() +
          ". Error response from solr: " + errorBody + " Original error: " + e.getMessage();
      log.error(msg,e);
      throw new IntegrationServiceException(msg,e);
    } catch (WebClientException e) {
      String msg = "Failed to commit core " + coreName +
          ". Cause: " + e.getMessage();
      log.error(msg, e);
      throw new IntegrationServiceException(msg, e);
    }
  }

  /**
   * Gets document count in Solr core.
   *
   * @param coreName Solr core name
   * @return Number of documents in core
   */
  public long getDocumentCount(String coreName) {
    String queryUrl = SOLR_SINGLE_CORE_API_ENDPOINT +  "/" + coreName + "/select?q=*:*&rows=0&wt=json";
    try {
      String response = webClient.get()
          .uri(queryUrl)
          .retrieve()
          .bodyToMono(String.class)
          .block();

      // Parse response to get numFound
      JsonNode root = OBJECT_MAPPER.readTree(response);
      return root.path("response").path("numFound").asLong(0);

    } catch (Exception e) {
      log.error("Failed to get document count for core: {}", coreName, e);
      return 0;
    }
  }

}
