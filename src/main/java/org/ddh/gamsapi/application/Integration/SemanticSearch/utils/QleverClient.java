package org.ddh.gamsapi.application.Integration.SemanticSearch.utils;

import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.infrastructure.System.configproperties.GAMSDockerDNS;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * HTTP client for communicating with a QLever triplestore instance.
 * <p>
 * Uses SPARQL 1.1 Update protocol to INSERT and DELETE triples.
 * QLever accepts SPARQL Update via {@code Content-Type: application/sparql-update}
 * with the SPARQL string directly in the request body (NOT form-encoded).
 * <p>
 * Important QLever constraint (as of 2025): only a single update operation
 * per request is allowed. This means DROP and INSERT must be separate requests.
 */
@Slf4j
@Component
public class QleverClient {

  private final RestTemplate restTemplate;
  private final GAMSDockerDNS configProperties;

  /**
   * Content type for SPARQL 1.1 Update requests.
   * QLever expects the raw SPARQL string in the body with this content type.
   */
  private static final MediaType SPARQL_UPDATE_CONTENT_TYPE =
      MediaType.valueOf("application/sparql-update");

  public QleverClient(GAMSDockerDNS configProperties) {
    this.configProperties = configProperties;
    this.restTemplate = new RestTemplate();
  }


  /**
   * Sends a SPARQL UPDATE request to QLever.
   *
   * @param sparql  the SPARQL Update string (e.g. INSERT DATA, DROP GRAPH, CLEAR GRAPH)
   * @param context description of the operation for logging (e.g. project abbreviation)
   * @throws IOException if the request fails or returns an error status
   */
  public void postSparqlUpdate(String sparql, String context) throws IOException {

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(SPARQL_UPDATE_CONTENT_TYPE);

    HttpEntity<String> request = new HttpEntity<>(sparql, headers);

    ResponseEntity<String> response;
    try {
      String updateUrl = configProperties.getQleverUrl();
      response = restTemplate.postForEntity(updateUrl, request, String.class);
    } catch (RestClientException e) {
      String msg = String.format(
          "Failed to send SPARQL Update to QLever. Context: %s. Cause: %s",
          context, e.getMessage()
      );
      log.error(msg, e);
      throw new IOException(msg, e);
    }

    if (response.getStatusCode().isError()) {
      String msg = String.format(
          "QLever returned error for SPARQL Update. Context: %s. Status: %s. Body: %s",
          context, response.getStatusCode(), response.getBody()
      );
      log.error(msg);
      throw new IOException(msg);
    }

    log.trace("Successfully sent SPARQL Update to QLever. Context: {}, Status: {}",
        context, response.getStatusCode());
  }

  /**
   * Inserts triples into a named graph via SPARQL UPDATE INSERT DATA.
   * <p>
   * Prefixes (already in SPARQL syntax, i.e. {@code PREFIX foo: <...>}) are
   * placed before the INSERT DATA block. The triples string may use prefixed
   * names that are declared in the provided prefixes.
   * <p>
   * Produces:
   * <pre>
   * PREFIX foo: &lt;http://example.org/&gt;
   * PREFIX bar: &lt;http://example.org/bar#&gt;
   * INSERT DATA { GRAPH &lt;graphUri&gt; {
   *   ...triples...
   * } }
   * </pre>
   *
   * @param graphUri        the named graph URI to insert into
   * @param sparqlPrefixes  deduplicated PREFIX declarations in SPARQL syntax
   * @param triples         serialized triples (may use prefixed names declared in sparqlPrefixes)
   * @param context         description for logging
   * @throws IOException if the request fails
   */
  public void insertDataIntoGraph(String graphUri, Set<String> sparqlPrefixes,
                                  String triples, String context) throws IOException {
    String prefixBlock = sparqlPrefixes.stream()
        .sorted() // deterministic ordering for debuggability
        .collect(Collectors.joining("\n"));

    String sparql = String.format(
        "%s\nINSERT DATA { GRAPH <%s> {\n%s\n} }",
        prefixBlock, graphUri, triples
    );

    postSparqlUpdate(sparql, context);
  }

  /**
   * Drops (deletes) an entire named graph from QLever.
   * Uses DROP SILENT to avoid errors if the graph does not exist.
   *
   * @param graphUri the named graph URI to drop
   * @param context  description for logging
   * @throws IOException if the request fails
   */
  public void dropGraph(String graphUri, String context) throws IOException {
    String sparql = String.format("DROP SILENT GRAPH <%s>", graphUri);
    postSparqlUpdate(sparql, context);
  }

  /**
   * Clears all triples from a named graph without removing the graph itself.
   * Uses CLEAR SILENT to avoid errors if the graph does not exist.
   *
   * @param graphUri the named graph URI to clear
   * @param context  description for logging
   * @throws IOException if the request fails
   */
  public void clearGraph(String graphUri, String context) throws IOException {
    String sparql = String.format("CLEAR SILENT GRAPH <%s>", graphUri);
    postSparqlUpdate(sparql, context);
  }

}