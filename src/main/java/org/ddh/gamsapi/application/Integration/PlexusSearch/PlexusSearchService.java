package org.ddh.gamsapi.application.Integration.PlexusSearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationDataProcessingException;
import org.ddh.gamsapi.application.Integration.Common.interfaces.ClientManagedIntegrationService;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrClient;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrDocument;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrGamsCores;
import org.ddh.gamsapi.application.Integration.PlexusSearch.dto.PlexusSearchQueryRequestDto;
import org.ddh.gamsapi.application.Integration.PlexusSearch.dto.PlexusSearchResponseDto;
import org.ddh.gamsapi.application.Integration.PlexusSearch.validation.PlexusSearchQueryValidator;
import org.ddh.gamsapi.domain.Datastream.DatastreamId;
import org.ddh.gamsapi.domain.Datastream.utils.exceptions.DatastreamCannotLoadFileException;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamIndexingView;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlexusSearchService implements ClientManagedIntegrationService {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;
  private final IDatastreamContentRepository datastreamContentRepository;
  private final SolrClient solrClient;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final PlexusSearchQueryValidator plexusSearchQueryValidator;

  private final String SHARED_CORE_NAME = SolrGamsCores.PLEXUS_SEARCH_CORE.value;

  private static final int DEFAULT_BATCH_SIZE = 500;
  private static final int COMMIT_INTERVAL = 10; // Commit every 10 batches

  @Override
  public void indexObjects(String projectAbbr) {
    log.info("*** {}: Starting Plexus indexing for project: {})",
        this.getClass().getSimpleName(), projectAbbr);

    // Record the start time
    LocalDateTime startTime = LocalDateTime.now();

    // Find all datastreams for this project
    Page<IDatastreamIndexingView> page = datastreamRepository.findAllByDsidAndProject(
        PlexusSearchProperties.DATASTREAM_DSID.name,
        projectAbbr,
        PageRequest.of(0, Integer.MAX_VALUE)
    );

    if (page.isEmpty()) {
      log.info("No Plexus datastreams found for project: {}", projectAbbr);
      return;
    }

    log.info("Found {} Plexus datastreams for project {}",
        page.getTotalElements(), projectAbbr);


    int batchCount = 0;
    int objectsProcessed = 0;
    int totalDocumentsIndexed = 0;
    List<String> warnings = new ArrayList<>();

    List<SolrDocument> currentBatch = new ArrayList<>(DEFAULT_BATCH_SIZE);

    for (IDatastreamIndexingView datastreamView : page.getContent()) {
      objectsProcessed++;

      DatastreamId datastreamId = DatastreamId.builder()
          .dsid(datastreamView.getDsid())
          .digitalObject(datastreamView.getDigitalObject().getId())
          .build();

      try {
        // Load datastream content
        InputStreamResource resource = datastreamContentRepository.findById(datastreamId);

        // Parse Solr documents from datastream
        SolrDocument[] solrDocuments = SolrDocument.from(resource);

        // Validate and enrich documents
        solrDocuments = validateAndEnrichDocuments(solrDocuments, projectAbbr, datastreamId.getDigitalObject());

        // Add to batch
        for (SolrDocument document : solrDocuments) {
          currentBatch.add(document);
          totalDocumentsIndexed++;
        }

        // Flush batch if full
        if (currentBatch.size() >= DEFAULT_BATCH_SIZE) {
          // CHANGED: Post to shared core
          solrClient.post(SolrGamsCores.PLEXUS_SEARCH_CORE.value, currentBatch.toArray(new SolrDocument[0]), false);
          currentBatch.clear();
          batchCount++;

          // Commit every N batches
          if (batchCount % COMMIT_INTERVAL == 0) {
            solrClient.commit(SolrGamsCores.PLEXUS_SEARCH_CORE.value);
            log.info("Committed batch {} for project {} to shared core {} ({} objects processed)",
                batchCount, projectAbbr, SolrGamsCores.PLEXUS_SEARCH_CORE.value, objectsProcessed);
          }
        }

      } catch (IOException e) {
        String msg = String.format("Failed to read Plexus datastream %s: %s",
            datastreamId, e.getMessage());
        log.error(msg);
        // TODO exception?
        warnings.add(msg);
      } catch (Exception e) {
        String msg = String.format("Error processing Plexus datastream %s: %s",
            datastreamId, e.getMessage());
        log.error(msg, e);
        // TODO exception?
        warnings.add(msg);
      }
    }

    // Post remaining documents
    if (!currentBatch.isEmpty()) {
      solrClient.post(SolrGamsCores.PLEXUS_SEARCH_CORE.value, currentBatch.toArray(new SolrDocument[0]), false);
      batchCount++;
    }

    // Final commit
    solrClient.commit(SolrGamsCores.PLEXUS_SEARCH_CORE.value);
    log.info("Final commit completed for project {} in shared core {}", projectAbbr, SolrGamsCores.PLEXUS_SEARCH_CORE.value);

    // TODO think about - instead of failing the whole process on commit error, collect warnings and then fail at the end?
    // alternatively collecting warnings
//    try {
//      solrClient.commit(SolrGamsCores.PLEXUS_SEARCH_CORE.value);
//      log.info("Final commit completed for project {} in shared core {}", projectAbbr, SolrGamsCores.PLEXUS_SEARCH_CORE.value);
//    } catch (Exception e) {
//      String msg = "Final commit failed: " + e.getMessage();
//      log.error(msg);
//      warnings.add(msg);
//    }

    LocalDateTime endTime = LocalDateTime.now();
    long elapsedMs = java.time.Duration.between(startTime, endTime).toMillis();

    log.info("Plexus indexing completed for project {}: {} documents in {} batches, {}ms (shared core: {})",
        projectAbbr, totalDocumentsIndexed, batchCount, elapsedMs, SolrGamsCores.PLEXUS_SEARCH_CORE.value);

    if (!warnings.isEmpty()) {
      log.warn("Plexus indexing completed with {} warnings", warnings.size());
    }

  }

  @Override
  public void deleteIndexedObjects(String projectAbbr) {

    final String DELETION_QUERY = String.format("%s:%s",
        PlexusSearchProperties.ENTITY_PROJECT_ABBR.name,
        projectAbbr);

    solrClient.delete(SolrGamsCores.PLEXUS_SEARCH_CORE.value, DELETION_QUERY);

  }

  @Override
  public void indexObject(String projectAbbr, String id) {

    final var PLEXUS_SEARCH_JSON_DSID = DatastreamId.builder()
        .digitalObject(id)
        .dsid(PlexusSearchProperties.DATASTREAM_DSID.name)
        .build();

    var datastreamAsStream = datastreamContentRepository.findById(PLEXUS_SEARCH_JSON_DSID);

    SolrDocument[] solrDocuments;
    try {
      solrDocuments = SolrDocument.from(datastreamAsStream);
    } catch (IOException e) {
      String msg = String.format("Failed to read datastream content %s for datastream %s. Original error: %s", datastreamAsStream.getDescription(), PLEXUS_SEARCH_JSON_DSID, e);
      log.error(msg);
      throw new DatastreamCannotLoadFileException(msg);
    }

    // TODO post batch size of documents? (might be a lot lof solr documents in a single object)
    // currently posting all documents at once
    solrClient.post(SolrGamsCores.PLEXUS_SEARCH_CORE.value,  solrDocuments);
    // commit all documents at once?
    solrClient.commit(SolrGamsCores.PLEXUS_SEARCH_CORE.value);

  }

  @Override
  public void deleteIndexedObject(String projectAbbr, String id) {

    final String DELETION_QUERY = String.format("%s:%s",
        PlexusSearchProperties.ENTITY_OBJECT_ID.name,
        id
    );

    solrClient.delete(SolrGamsCores.PLEXUS_SEARCH_CORE.value,  DELETION_QUERY);
    log.info("Deleted Plexus indexed documents for object ID {} in project {} from core {}",
        id, projectAbbr, SolrGamsCores.PLEXUS_SEARCH_CORE.value);

  }


  /**
   * Executes a Plexus search query.
   * CRITICAL: Always adds mandatory project filter for tenant isolation in shared core.
   *
   * @param projectAbbr Project abbreviation
   * @param request Query request
   * @return Search response with results and metadata
   */
  public PlexusSearchResponseDto search(String projectAbbr, PlexusSearchQueryRequestDto request) {
    log.debug("Executing Plexus search for project {} in core {}: {}",
        projectAbbr, SHARED_CORE_NAME, request.getQuery());

    long startTime = System.currentTimeMillis();

    // 1. Check query quota
    // quotaManager.checkQueryQuota(projectAbbr);
    // TODO implement quota manager later when needed

    // 2. Validate query
    plexusSearchQueryValidator.validateQuery(request, projectAbbr);

    // 3. Build Solr query URL
    String solrUrl = PlexusSearchSolrQueryBuilder.buildSolrQueryUrl(
        SHARED_CORE_NAME,
        request,
        Set.of(projectAbbr)
    );

    log.info("Plexus query URL: {}", solrUrl);

    // 4. Execute query
    // TODO would be better if solrClient returns a SolrResponse class?
    // (could be overengineered - because: solr might return different responses for different queries)
    String responseJson = solrClient.get(solrUrl);

    // 5. Parse response
    var response = PlexusSearchResponseDto.from(responseJson, request);

    // 6. Add metadata to response
    long elapsedMs = System.currentTimeMillis() - startTime;
    response.setExecutionTimeMs(elapsedMs);

    // 7. Add hints for query optimization
    List<String> hints = generateQueryHints(request, response);
    response.setHints(hints);

    log.debug("Plexus search completed for project {} in shared core {} in {}ms: {} results",
        projectAbbr, SHARED_CORE_NAME, elapsedMs, response.getTotalCount());

    return response;
  }


  /**
   * Validates and enriches Solr documents with required fields.
   */
  private SolrDocument[] validateAndEnrichDocuments(
      SolrDocument[] documents,
      String projectAbbr,
      String objectId
  ) {
    for (SolrDocument doc : documents) {
      // Validate required fields
      String id = (String) doc.getProperty(PlexusSearchProperties.ENTITY_ID.name);
      if (id == null || id.isEmpty()) {
        throw new IntegrationDataProcessingException(
            String.format("Missing required field '%s' in Plexus document",
                PlexusSearchProperties.ENTITY_ID.name));
      }

      // Validate ID format (must start with project abbreviation)
      if (!id.startsWith(projectAbbr + ".")) {
        throw new IntegrationDataProcessingException(
            String.format("Document ID '%s' must start with project abbreviation '%s.'",
                id, projectAbbr));
      }

      // CRITICAL: Add mandatory project field for tenant isolation in shared core
      doc.addProperty(PlexusSearchProperties.ENTITY_PROJECT_ABBR.name, projectAbbr);

      // Ensure mandatory object ID reference is present
      doc.addProperty(PlexusSearchProperties.ENTITY_OBJECT_ID.name, objectId);

    }

    return documents;
  }

  /**
   * Generates query optimization hints based on query patterns.
   */
  private List<String> generateQueryHints(PlexusSearchQueryRequestDto request, PlexusSearchResponseDto response) {
    List<String> hints = new ArrayList<>();

    // Hint for deep pagination
    if (request.getStart() > 10000) {
      hints.add("Deep pagination detected (start > 10000). Consider using cursor-based pagination for better performance.");
    }

    // Hint for large result sets without filters
    if (response.getTotalCount() > 100000 &&
        (request.getFilterQueries() == null || request.getFilterQueries().size() <= 1)) {
      hints.add("Large result set without filters. Consider adding filter queries to narrow results.");
    }

    // Hint for slow queries
    if (response.getExecutionTimeMs() != null && response.getExecutionTimeMs() > 1000) {
      hints.add("Query took >1s. Consider adding filters, reducing result size, or optimizing query structure.");
    }

    if(hints.isEmpty()){
      hints.add("No optimization hints. Query looks good!");
    }

    return hints;
  }


}