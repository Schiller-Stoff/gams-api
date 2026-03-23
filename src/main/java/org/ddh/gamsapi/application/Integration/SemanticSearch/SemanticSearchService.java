package org.ddh.gamsapi.application.Integration.SemanticSearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.Common.interfaces.ClientManagedIntegrationService;
import org.ddh.gamsapi.application.Integration.SemanticSearch.utils.QleverClient;
import org.ddh.gamsapi.application.Integration.SemanticSearch.utils.TurtleParseResult;
import org.ddh.gamsapi.application.Integration.SemanticSearch.utils.TurtleSparqlConverter;
import org.ddh.gamsapi.domain.Datastream.DatastreamId;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamIndexingView;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Service for semantic search integration via QLever triplestore.
 * <p>
 * Indexes Turtle datastreams ({@code SEMANTIC_STATEMENTS.ttl}) from digital objects
 * into QLever using SPARQL 1.1 UPDATE (INSERT DATA) batched per project.
 * <p>
 * Each project's triples are stored in a dedicated named graph:
 * {@code <https://gams.uni-graz.at/project/{projectAbbr}>}
 * <p>
 * The batch strategy groups multiple objects' triples into a single INSERT DATA request
 * to minimize HTTP round-trips while staying within safe request-size limits.
 * Prefix declarations from individual Turtle files are converted to SPARQL syntax,
 * deduplicated across the batch, and prepended to the INSERT DATA block.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SemanticSearchService implements ClientManagedIntegrationService {

  private final IDatastreamRepository datastreamRepository;
  private final IDatastreamContentRepository datastreamContentRepository;
  private final QleverClient qleverClient;

  /**
   * Base URI for named graphs. Each project gets its own graph.
   */
  private static final String GRAPH_BASE_URI = "https://gams.uni-graz.at/project/";

  /**
   * Number of datastreams to fetch from the DB per page.
   */
  private static final int PAGE_SIZE = 500;

  /**
   * Number of objects whose triples are aggregated before sending one INSERT DATA request.
   * With ~10 triples per object at ~150 bytes each, 500 objects ≈ 750KB per request.
   */
  private static final int BATCH_SIZE = 500;


  /**
   * Indexes all semantic search datastreams for a project into QLever.
   * <p>
   * Strategy: DROP the project's named graph first (idempotent), then
   * iterate all SEMANTIC_STATEMENTS.ttl datastreams in pages,
   * aggregate their Turtle content in batches, and send each batch
   * as a single INSERT DATA request with deduplicated PREFIX declarations.
   *
   * @param projectAbbr the project abbreviation
   */
  @Override
  public void indexObjects(String projectAbbr) {

    Instant startTime = Instant.now();
    String graphUri = GRAPH_BASE_URI + projectAbbr;
    log.info("*** SemanticSearchService: Starting indexing for project: {} into graph <{}>",
        projectAbbr, graphUri);

    // 01. Drop existing graph to ensure idempotent rebuild
    try {
      qleverClient.dropGraph(graphUri, projectAbbr);
      log.info("Dropped existing graph <{}> for project {}", graphUri, projectAbbr);
    } catch (IOException e) {
      log.error("Failed to drop graph <{}> for project {}. Aborting indexing. Error: {}",
          graphUri, projectAbbr, e.getMessage());
      return;
    }

    // 02. Paginate through all SEMANTIC_STATEMENTS.ttl datastreams for this project
    int pageIndex = 0;
    int objectsProcessed = 0;
    int batchesSent = 0;
    List<String> warnings = new ArrayList<>();

    // Batch accumulators
    Set<String> batchPrefixes = new HashSet<>();
    StringBuilder batchTriples = new StringBuilder();
    int objectsInCurrentBatch = 0;

    Page<IDatastreamIndexingView> page;
    do {
      page = datastreamRepository.findAllByDsidAndProject(
          SemanticSearchProperties.DATASTREAM_DSID.name,
          projectAbbr,
          PageRequest.of(pageIndex, PAGE_SIZE)
      );

      if (pageIndex == 0 && page.isEmpty()) {
        log.info("No {} datastreams found for project: {}",
            SemanticSearchProperties.DATASTREAM_DSID.name, projectAbbr);
        return;
      }

      if (pageIndex == 0) {
        log.info("Found {} datastreams for semantic-search indexing for project {}",
            page.getTotalElements(), projectAbbr);
      }

      // 03. For each datastream in this page: read Turtle content, separate prefixes and triples
      for (IDatastreamIndexingView datastreamView : page.getContent()) {

        String digitalObjectId = datastreamView.getDigitalObject().getId();
        DatastreamId datastreamId = DatastreamId.builder()
            .dsid(datastreamView.getDsid())
            .digitalObject(digitalObjectId)
            .build();

        try {
          String turtleContent = readTurtleContent(datastreamId);
          TurtleParseResult parseResult = TurtleSparqlConverter.separatePrefixesAndTriples(turtleContent);

          if (!parseResult.hasNoTriples()) {
            // Merge prefixes from this file into the batch's prefix set (auto-deduplicates)
            batchPrefixes.addAll(parseResult.sparqlPrefixes());
            batchTriples.append("# Object: ").append(digitalObjectId).append("\n");
            batchTriples.append(parseResult.triples()).append("\n");
            objectsInCurrentBatch++;
          }
        } catch (Exception e) {
          String warning = String.format(
              "Failed to read datastream %s for object %s: %s",
              datastreamId, digitalObjectId, e.getMessage()
          );
          log.warn(warning);
          warnings.add(warning);
        }

        objectsProcessed++;

        // 04. Flush batch when it reaches the configured size
        if (objectsInCurrentBatch >= BATCH_SIZE) {
          batchesSent += flushBatch(batchPrefixes, batchTriples, graphUri, projectAbbr, batchesSent + 1);
          objectsInCurrentBatch = 0;
        }
      }

      pageIndex++;
    } while (page.hasNext());

    // 05. Flush remaining triples
    if (objectsInCurrentBatch > 0) {
      batchesSent += flushBatch(batchPrefixes, batchTriples, graphUri, projectAbbr, batchesSent + 1);
    }

    Duration duration = Duration.between(startTime, Instant.now());
    log.info("*** SemanticSearchService: Completed indexing for project {}. " +
            "Objects processed: {}, batches sent: {}, warnings: {}, duration: {}",
        projectAbbr, objectsProcessed, batchesSent, warnings.size(), duration);

    if (!warnings.isEmpty()) {
      log.warn("Indexing warnings for project {}: {}", projectAbbr, warnings);
    }
  }


  /**
   * Deletes all indexed data for a project by dropping its named graph.
   *
   * @param projectAbbr the project abbreviation
   */
  @Override
  public void deleteIndexedObjects(String projectAbbr) {
    String graphUri = GRAPH_BASE_URI + projectAbbr;
    log.info("Deleting semantic search index for project: {} (dropping graph <{}>)",
        projectAbbr, graphUri);
    try {
      qleverClient.dropGraph(graphUri, projectAbbr);
      log.info("Successfully dropped graph <{}> for project {}", graphUri, projectAbbr);
    } catch (IOException e) {
      log.error("Failed to drop graph <{}> for project {}. Error: {}",
          graphUri, projectAbbr, e.getMessage());
    }
  }


  /**
   * Indexes a single digital object's semantic statements into QLever.
   * <p>
   * Note: This does NOT drop the project graph — it adds triples to it.
   * For a full project rebuild, use {@link #indexObjects(String)}.
   *
   * @param projectAbbr project abbreviation
   * @param id          digital object ID
   */
  @Override
  public void indexObject(String projectAbbr, String id) {

    String graphUri = GRAPH_BASE_URI + projectAbbr;
    DatastreamId datastreamId = DatastreamId.builder()
        .dsid(SemanticSearchProperties.DATASTREAM_DSID.name)
        .digitalObject(id)
        .build();

    try {
      String turtleContent = readTurtleContent(datastreamId);
      TurtleParseResult parseResult = TurtleSparqlConverter.separatePrefixesAndTriples(turtleContent);

      if (parseResult.hasNoTriples()) {
        log.info("No triples found in {} for object {}", datastreamId, id);
        return;
      }

      qleverClient.insertDataIntoGraph(graphUri, parseResult.sparqlPrefixes(), parseResult.triples(),
          String.format("object %s in project %s", id, projectAbbr));

      log.info("Successfully indexed object {} into semantic search for project {}", id, projectAbbr);
    } catch (IOException e) {
      log.error("Failed to index object {} for project {} in semantic search. Error: {}",
          id, projectAbbr, e.getMessage());
    }
  }


  /**
   * Deletes a single object's triples from the project graph.
   * <p>
   * Uses DELETE WHERE to remove all triples where the object's URI appears as subject.
   * This assumes the object URI follows the GAMS convention: {@code <https://gams.uni-graz.at/{objectId}>}
   *
   * @param projectAbbr project abbreviation
   * @param id          digital object ID
   */
  @Override
  public void deleteIndexedObject(String projectAbbr, String id) {
    String graphUri = GRAPH_BASE_URI + projectAbbr;
    String objectUri = "https://gams.uni-graz.at/" + id;

    // TODO: Verify that QLever supports DELETE WHERE. If not, a workaround would be
    //  to re-index the entire project via indexObjects(projectAbbr) after object deletion.
    String sparql = String.format(
        "DELETE WHERE { GRAPH <%s> { <%s> ?p ?o } }",
        graphUri, objectUri
    );

    try {
      qleverClient.postSparqlUpdate(sparql,
          String.format("delete object %s from project %s", id, projectAbbr));
      log.info("Deleted object {} from semantic search for project {}", id, projectAbbr);
    } catch (IOException e) {
      log.error("Failed to delete object {} from semantic search for project {}. Error: {}",
          id, projectAbbr, e.getMessage());
    }
  }


  // ---------------------------------------------------------------------------
  // Private helpers
  // ---------------------------------------------------------------------------

  /**
   * Reads a Turtle datastream from the content repository and returns it as a String.
   *
   * @param datastreamId the datastream to read
   * @return the Turtle content as String
   * @throws IOException if reading fails
   */
  private String readTurtleContent(DatastreamId datastreamId) throws IOException {
    InputStreamResource resource = datastreamContentRepository.findById(datastreamId);
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
      return reader.lines().collect(Collectors.joining("\n"));
    }
  }

  /**
   * Sends the accumulated batch of triples as a single INSERT DATA request
   * with all collected PREFIX declarations.
   * <p>
   * Clears both the prefix set and the triple buffer after sending.
   *
   * @param prefixes    accumulated SPARQL PREFIX declarations for this batch
   * @param triples     accumulated triple data for this batch
   * @param graphUri    target named graph
   * @param projectAbbr project abbreviation for logging
   * @param batchNumber current batch number for logging
   * @return 1 if batch was sent successfully, 0 otherwise
   */
  private int flushBatch(Set<String> prefixes, StringBuilder triples,
                         String graphUri, String projectAbbr, int batchNumber) {
    if (triples.isEmpty()) {
      return 0;
    }

    try {
      qleverClient.insertDataIntoGraph(
          graphUri,
          prefixes,
          triples.toString(),
          String.format("project %s batch %d", projectAbbr, batchNumber)
      );
      log.debug("Sent batch {} for project {} to QLever ({} prefixes)",
          batchNumber, projectAbbr, prefixes.size());
      // Clear buffers after successful send
      triples.setLength(0);
      prefixes.clear();
      return 1;
    } catch (IOException e) {
      log.error("Failed to send batch {} for project {} to QLever. Error: {}",
          batchNumber, projectAbbr, e.getMessage());
      // Clear even on failure to prevent re-sending stale data
      triples.setLength(0);
      prefixes.clear();
      return 0;
    }
  }

}