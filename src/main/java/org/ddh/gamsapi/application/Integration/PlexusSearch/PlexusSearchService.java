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
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamIndexingView;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlexusSearchService implements ClientManagedIntegrationService {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;
  private final IDatastreamContentRepository datastreamContentRepository;
  private final SolrClient solrClient;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final PlexusSearchQueryValidator queryValidator;

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

    throw new UnsupportedOperationException("Single object indexing is not supported for PlexusSearch.");


  }

  @Override
  public void deleteIndexedObject(String projectAbbr, String id) {

    throw new UnsupportedOperationException("Single object indexing is not supported for PlexusSearch.");
  }


  /**
   * Executes a Plexus search query.
   *
   * CRITICAL: Always adds mandatory project filter for tenant isolation in shared core.
   *
   * @param projectAbbr Project abbreviation
   * @param request Query request
   * @return Search response with results and metadata
   */
  public PlexusSearchResponseDto search(String projectAbbr, PlexusSearchQueryRequestDto request) {
    log.debug("Executing Plexus search for project {} in core {}: {}",
        projectAbbr, SHARED_CORE_NAME, request.getQuery());

    // TODO add more logging to method

    long startTime = System.currentTimeMillis();

    // 1. Check query quota
    // TODO not implemented (not taken over atm - implement later if needed)
    //quotaManager.checkQueryQuota(projectAbbr);

    // 2. Add mandatory project filter (CRITICAL for tenant isolation in shared core)
    List<String> filterQueries = new ArrayList<>(request.getFilterQueries() != null ?
        request.getFilterQueries() : List.of());
    String projectFilter = PlexusSearchProperties.ENTITY_PROJECT_ABBR.name + ":" + projectAbbr;
    filterQueries.add(0, projectFilter); // Add as first filter

    // 3. Validate query
    // TODO change name of injected field queryValidator to plexusQueryValidator
    queryValidator.validateQuery(request, projectAbbr);

    // 4. Build Solr query URL (CHANGED: uses shared core)
    String solrUrl = buildSolrQueryUrl(SHARED_CORE_NAME, request, filterQueries);

    log.info("Plexus query URL (shared core): {}", solrUrl);

    // 5. Execute query
    // TODO would be better if solrClient returns a SolrResponse class?
    // (could be overengineered - because: solr might return different responses for different queries)
    String responseJson = solrClient.get(solrUrl);

    // 6. Parse response
    var response = PlexusSearchResponseDto.from(responseJson, request);

    // 7. Add metadata
    long elapsedMs = System.currentTimeMillis() - startTime;
    response.setExecutionTimeMs(elapsedMs);
    // TODO warnings not implemented
    // response.setWarnings(warnings.isEmpty() ? null : warnings);
//
//    // 8. Add hints for query optimization
//    List<String> hints = generateQueryHints(request, response);
//    response.setHints(hints.isEmpty() ? null : hints);
//
//    log.debug("Plexus search completed for project {} in shared core {} in {}ms: {} results",
//        projectAbbr, SHARED_CORE_NAME, elapsedMs, response.getTotalResults());




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
   * Builds Solr query URL from request parameters.
   */
  private String buildSolrQueryUrl(
      String coreName,
      PlexusSearchQueryRequestDto request,
      List<String> filterQueries
  ) {
    // TODO move logic to own class PlexusSearchSolrQueryBuilder.java?

    StringBuilder url = new StringBuilder();

    // TODO use gamsDockerDns or config value
    url.append("/solr/").append(coreName).append("/select");
    url.append("?q=").append(encodeQueryParam(request.getQuery()));
    url.append("&rows=").append(request.getRows());
    url.append("&start=").append(request.getStart());

    // Add filter queries (includes mandatory project filter)
    for (String fq : filterQueries) {
      url.append("&fq=").append(encodeQueryParam(fq));
    }

    // Add sort
    if (request.getSort() != null && !request.getSort().isEmpty()) {
      url.append("&sort=").append(encodeQueryParam(request.getSort()));
    }

    // Add highlighting
    if (Boolean.TRUE.equals(request.getHighlight())) {
      url.append("&hl=true");
      if (request.getHighlightFields() != null && !request.getHighlightFields().isEmpty()) {
        url.append("&hl.fl=").append(String.join(",", request.getHighlightFields()));
      }
    }

    // Add faceting
    if (request.getFacetFields() != null && !request.getFacetFields().isEmpty()) {
      url.append("&facet=true");
      url.append("&facet.field=").append(String.join("&facet.field=", request.getFacetFields()));
      url.append("&facet.limit=").append(request.getFacetLimit());
    }

    // Add cursor mark if provided
    if (request.getCursorMark() != null && !request.getCursorMark().isEmpty()) {
      url.append("&cursorMark=").append(encodeQueryParam(request.getCursorMark()));
      // Cursor pagination requires a sort with unique field
      if (request.getSort() == null || request.getSort().isEmpty()) {
        url.append("&sort=").append(PlexusSearchProperties.ENTITY_ID.name).append(" asc");
      }
    }

    return url.toString();
  }


  /**
   * URL-encodes query parameter values.
   */
  private String encodeQueryParam(String value) {
    try {
      return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    } catch (Exception e) {
      return value; // Fallback
    }
  }


}