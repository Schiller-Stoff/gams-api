package org.ddh.gamsapi.application.Integration.SemanticSearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.Common.interfaces.ClientManagedIntegrationService;
import org.ddh.gamsapi.application.Integration.SemanticSearch.utils.QLeverBulkExporter;
import org.ddh.gamsapi.application.Integration.SemanticSearch.utils.QleverClient;
import org.ddh.gamsapi.application.Integration.SemanticSearch.utils.TurtleParseResult;
import org.ddh.gamsapi.application.Integration.SemanticSearch.utils.TurtleSparqlConverter;
import org.ddh.gamsapi.domain.Datastream.DatastreamId;
import org.ddh.gamsapi.domain.Datastream.utils.exceptions.DatastreamNotFoundException;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamIndexingView;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
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
 * Provides two indexing strategies:
 * <ul>
 *   <li><b>SPARQL UPDATE (HTTP)</b> — {@link #indexObjects(String)}, {@link #indexObject(String, String)}:
 *       Sends triples directly via SPARQL 1.1 UPDATE. Good for small-to-medium datasets (&lt;2M triples)
 *       and single-object operations. Data is immediately available for queries.</li>
 *   <li><b>Bulk file export</b> — {@link #exportProject(String)}, {@link #exportAllProjects()}:
 *       Exports Turtle datastreams to .nq.gz files on a shared volume for QLever's bulk indexer.
 *       Required for large datasets (50M+ triples). Needs a subsequent QLever index rebuild
 *       and server restart to become queryable.</li>
 * </ul>
 * <p>
 * Each project's triples are stored in a dedicated named graph:
 * {@code <https://gams.uni-graz.at/project/{projectAbbr}>}
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SemanticSearchService implements ClientManagedIntegrationService {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;
  private final IDatastreamContentRepository datastreamContentRepository;
  private final QleverClient qleverClient;
  private final QLeverBulkExporter bulkExporter;

  /**
   * Base URI for named graphs. Each project gets its own graph.
   */
  static final String GRAPH_BASE_URI = "https://gams.uni-graz.at/project/";

  /**
   * Number of datastreams to fetch from the DB per page.
   */
  private static final int PAGE_SIZE = 500;

  /**
   * Number of objects whose triples are aggregated before sending one INSERT DATA request.
   */
  private static final int BATCH_SIZE = 500;


  // ===========================================================================
  // SPARQL UPDATE path — for small/medium datasets and single-object operations
  // ===========================================================================

  @Override
  public void indexObjects(String projectAbbr) {

    Instant startTime = Instant.now();
    String graphUri = GRAPH_BASE_URI + projectAbbr;
    log.info("*** SemanticSearchService: Starting SPARQL UPDATE indexing for project: {} into graph <{}>",
        projectAbbr, graphUri);

    try {
      qleverClient.dropGraph(graphUri, projectAbbr);
      log.info("Dropped existing graph <{}> for project {}", graphUri, projectAbbr);
    } catch (IOException e) {
      log.error("Failed to drop graph <{}> for project {}. Aborting indexing. Error: {}",
          graphUri, projectAbbr, e.getMessage());
      return;
    }

    int pageIndex = 0;
    int objectsProcessed = 0;
    int batchesSent = 0;
    List<String> warnings = new ArrayList<>();

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
            batchPrefixes.addAll(parseResult.sparqlPrefixes());
            batchTriples.append("# Object: ").append(digitalObjectId).append("\n");
            batchTriples.append(parseResult.triples()).append("\n");
            objectsInCurrentBatch++;
          }
        } catch (Exception e) {
          String warning = String.format("Failed to read datastream %s for object %s: %s",
              datastreamId, digitalObjectId, e.getMessage());
          log.warn(warning);
          warnings.add(warning);
        }

        objectsProcessed++;

        if (objectsInCurrentBatch >= BATCH_SIZE) {
          batchesSent += flushBatch(batchPrefixes, batchTriples, graphUri, projectAbbr, batchesSent + 1);
          objectsInCurrentBatch = 0;
        }
      }

      pageIndex++;
    } while (page.hasNext());

    if (objectsInCurrentBatch > 0) {
      batchesSent += flushBatch(batchPrefixes, batchTriples, graphUri, projectAbbr, batchesSent + 1);
    }

    try {
      qleverClient.rebuildIndex();
      // TODO better error handling
    } catch (IOException e) {
      log.error("Failed to trigger QLever index rebuild. Error: {}", e.getMessage());
      // TODO own errors?
      return;
    }


    Duration duration = Duration.between(startTime, Instant.now());
    log.info("*** SemanticSearchService: Completed SPARQL UPDATE indexing for project {}. " +
            "Objects processed: {}, batches sent: {}, warnings: {}, duration: {}",
        projectAbbr, objectsProcessed, batchesSent, warnings.size(), duration);

    if (!warnings.isEmpty()) {
      log.warn("Indexing warnings for project {}: {}", projectAbbr, warnings);
    }
  }


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


  @Override
  public void indexObject(String projectAbbr, String id) {

    if(!digitalObjectRepository.existsById(id)){
      throw new DigitalObjectNotFoundException(
          "Failed to index object " + id + " to the semantic search service. Digital object does not exist"
      );
    }

    String graphUri = GRAPH_BASE_URI + projectAbbr;
    DatastreamId datastreamId = DatastreamId.builder()
        .dsid(SemanticSearchProperties.DATASTREAM_DSID.name)
        .digitalObject(id)
        .build();

    if(!datastreamContentRepository.exists(datastreamId)){
      throw new DatastreamNotFoundException(
          "Failed to index object " + id + " to the semantic search service. Datastream " + datastreamId + " does not exist"
      );
    }

    try {
      String turtleContent = readTurtleContent(datastreamId);
      TurtleParseResult parseResult = TurtleSparqlConverter.separatePrefixesAndTriples(turtleContent);

      if (parseResult.hasNoTriples()) {
        log.info("No triples found in {} for object {}", datastreamId, id);
        return;
      }

      qleverClient.insertDataIntoGraph(graphUri, parseResult.sparqlPrefixes(), parseResult.triples(),
          String.format("object %s in project %s", id, projectAbbr));


      qleverClient.rebuildIndex();

      log.info("Successfully indexed object {} into semantic search for project {}", id, projectAbbr);
    } catch (IOException e) {
      log.error("Failed to index object {} for project {} in semantic search. Error: {}",
          id, projectAbbr, e.getMessage());
    }
  }


  @Override
  public void deleteIndexedObject(String projectAbbr, String id) {
    String graphUri = GRAPH_BASE_URI + projectAbbr;
    String objectUri = "https://gams.uni-graz.at/" + id;

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


  // ===========================================================================
  // Bulk file export path — for large datasets (50M+ triples)
  // ===========================================================================

  /**
   * Exports a single project's semantic search datastreams to a .nq.gz file
   * on the shared export volume for QLever bulk indexing.
   * <p>
   * This only writes the export file — it does NOT trigger a QLever index rebuild.
   * After exporting all desired projects, a QLever index rebuild and server restart
   * must be triggered separately (see {@link #rebuildQleverIndex()}).
   * <p>
   * Typical workflow:
   * <pre>
   * semanticSearchService.exportProject("projectA");
   * semanticSearchService.exportProject("projectB");
   * semanticSearchService.rebuildQleverIndex();
   * </pre>
   *
   * @param projectAbbr the project abbreviation
   * @return export report with statistics
   */
  public QLeverBulkExporter.ExportReport exportProject(String projectAbbr) {
    return bulkExporter.exportProject(projectAbbr);
  }


  /**
   * Exports ALL projects' semantic search datastreams to individual .nq.gz files
   * on the shared export volume.
   * <p>
   * This only writes the export files — it does NOT trigger a QLever index rebuild.
   *
   * @return list of per-project export reports
   */
  public List<QLeverBulkExporter.ExportReport> exportAllProjects() {
    return bulkExporter.exportAll();
  }


  /**
   * Logs instructions for triggering a QLever index rebuild from the exported files.
   * <p>
   * TODO: Automate the rebuild trigger. Current options:
   *  <ul>
   *    <li>Docker Compose: {@code docker exec} into the QLever container</li>
   *    <li>Sidecar container with a rebuild script watching for a trigger file</li>
   *    <li>HTTP management endpoint on the QLever container</li>
   *  </ul>
   *  For now, logs the manual commands to execute.
   */
  public void rebuildQleverIndex() {
    log.info("*** QLever index rebuild required. Export files are at: {}", bulkExporter.getExportPath());
    log.info("*** To rebuild manually, run:");
    log.info("***   1. docker compose stop semanticSearch");
    log.info("***   2. docker compose run --rm semanticSearch bash -c " +
        "'/qlever/qlever-index -i /index/gams -s /data/settings.json " +
        "-F ttl -f <(zcat /export/*.nq.gz) -W'");
    log.info("***   3. docker compose start semanticSearch");
  }


  /**
   * Full bulk reindex for a single project: export + rebuild instructions.
   * <p>
   * Note: Since QLever's file-based index is global (all projects in one index),
   * a single-project re-export still requires a full index rebuild.
   * For incremental single-project updates without rebuild, use
   * {@link #indexObjects(String)} (SPARQL UPDATE path) instead.
   *
   * @param projectAbbr the project abbreviation
   * @return export report
   */
  public QLeverBulkExporter.ExportReport exportAndRebuildProject(String projectAbbr) {
    QLeverBulkExporter.ExportReport report = exportProject(projectAbbr);
    rebuildQleverIndex();
    return report;
  }


  /**
   * Full bulk reindex for all projects: export all + rebuild instructions.
   *
   * @return list of per-project export reports
   */
  public List<QLeverBulkExporter.ExportReport> exportAndRebuildAll() {
    List<QLeverBulkExporter.ExportReport> reports = exportAllProjects();
    rebuildQleverIndex();
    return reports;
  }


  // ---------------------------------------------------------------------------
  // Private helpers
  // ---------------------------------------------------------------------------

  private String readTurtleContent(DatastreamId datastreamId) throws IOException {
    InputStreamResource resource = datastreamContentRepository.findById(datastreamId);
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
      return reader.lines().collect(Collectors.joining("\n"));
    }
  }

  private int flushBatch(Set<String> prefixes, StringBuilder triples,
                         String graphUri, String projectAbbr, int batchNumber) {
    if (triples.isEmpty()) {
      return 0;
    }

    try {
      qleverClient.insertDataIntoGraph(
          graphUri, prefixes, triples.toString(),
          String.format("project %s batch %d", projectAbbr, batchNumber)
      );
      log.debug("Sent batch {} for project {} to QLever ({} prefixes)",
          batchNumber, projectAbbr, prefixes.size());
      triples.setLength(0);
      prefixes.clear();
      return 1;
    } catch (IOException e) {
      log.error("Failed to send batch {} for project {} to QLever. Error: {}",
          batchNumber, projectAbbr, e.getMessage());
      triples.setLength(0);
      prefixes.clear();
      return 0;
    }
  }

}