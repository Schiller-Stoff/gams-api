package org.ddh.gamsapi.application.Integration.CustomSearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationDataProcessingException;
import org.ddh.gamsapi.application.Integration.Common.interfaces.ClientManagedIntegrationService;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrClient;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrDocument;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrGamsCores;
import org.ddh.gamsapi.domain.Datastream.DatastreamId;
import org.ddh.gamsapi.domain.Datastream.utils.exceptions.DatastreamCannotLoadFileException;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamIndexingView;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service for custom search integration.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomSearchService implements ClientManagedIntegrationService {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;
  private final IDatastreamContentRepository datastreamContentRepository;
  private final IDublinCoreEntryRepository dublinCoreEntryRepository;


  private final SolrClient solrClient;


  @Override
  public void indexObjects(String projectAbbr) {

    log.info("*** {}: Starting project indexing for: {}", this.getClass().getName(), projectAbbr);

    var page = datastreamRepository.findAllByDsidAndProject(
        CustomSearchProperties.DATASTREAM_DSID.name,
        projectAbbr,
        PageRequest.of(0, 1000000000)
    );

    if (page.isEmpty()) {
      log.info("No digital objects found for project: {}", projectAbbr);
      return;
    }

    log.info("Found {} datastreams for custom fulltext-indexing for project {}", page.getTotalElements(), projectAbbr);


    /////
    //
    // BATCH INDEX ALL fulltext documents together

    if (page.getTotalElements() == 0) {
      log.info("No digital objects with FULLTEXT_INDEX.json datastream found for project: {}", projectAbbr);
      return;
    }

    int batchCount = 0;
    int objectsProcessed = 0;

    // TODO this should be configurable?
    final int DEFAULT_BATCH_SIZE = 500;


    List<SolrDocument> currentBatch = new ArrayList<>(DEFAULT_BATCH_SIZE);
    for (IDatastreamIndexingView datastreamIndexingView : page.getContent()) {
      objectsProcessed++;

      DatastreamId datastreamId = DatastreamId.builder()
          .dsid(datastreamIndexingView.getDsid())
          .digitalObject(datastreamIndexingView.getDigitalObject().getId())
          .build();

      // load datastream content
      InputStreamResource resource = datastreamContentRepository.findById(datastreamId);

      // parse datastream content as solrDocuments array
      SolrDocument[] solrDocuments;
      try {
        solrDocuments = SolrDocument.from(resource);
        // validates that required properties are set for the custom search
        solrDocuments = validateSolrDocuments(solrDocuments, projectAbbr, datastreamId.getDigitalObject());
      } catch (IOException e) {
        String msg = String.format("Failed to read datastream content %s for datastream %s. Original error: %s", resource.getDescription(), datastreamId, e);
        log.error(msg);
        throw new DatastreamCannotLoadFileException(msg);
      }

      //02. add to batch
      for (SolrDocument document : solrDocuments) {
        currentBatch.add(document);
      }

      //03. After reaching batch size, post to solr + clear batch (with commit)
      // Flush batch if it's getting full (but don't commit yet)
      if (currentBatch.size() >= DEFAULT_BATCH_SIZE) {
        log.info("Processed {} custom search objects from datastream {} - current batch: {}", currentBatch.size(), datastreamId, currentBatch);
        solrClient.post(SolrGamsCores.CUSTOM_SEARCH_CORE.value, currentBatch.toArray(new SolrDocument[0]));
        currentBatch.clear();
        batchCount++;
        log.info("Processed batch {} for fulltext core ({} objects processed so far)", batchCount, objectsProcessed);
      }

    }

    // Post remaining documents in batch (e.g. when batch size not reached at the end / or never reached)
    if (!currentBatch.isEmpty()) {
      solrClient.post(SolrGamsCores.CUSTOM_SEARCH_CORE.value, currentBatch.toArray(new SolrDocument[0]));
    }


    // Final commit
    try {
      solrClient.commit(SolrGamsCores.CUSTOM_SEARCH_CORE.value);
      log.info("Final commit completed for fulltext core");
    } catch (Exception e) {
      //stats.addWarning("Final commit failed: " + e.getMessage());
    }

  }

  /**
   * TODO test
   * Delete all indexed objects for a given project.
   *
   * @param projectAbbr Project abbreviation
   */
  @Override
  public void deleteIndexedObjects(String projectAbbr) {
    final String DELETE_QUERY = String.format("%s:%s", CustomSearchProperties.ENTITY_PROJECT_ABBR.name, projectAbbr);
    solrClient.delete(
        SolrGamsCores.CUSTOM_SEARCH_CORE.value,
        DELETE_QUERY
    );
    log.info("Deled all indexed objects for project: {} from custom search core", projectAbbr);
  }

  /**
   * Indexes a single object for custom search.
   * @param projectAbbr project to be indexed
   * @param id          id of the object to be indexed
   */
  @Override
  public void indexObject(String projectAbbr, String id) {

    final var CUSTOM_SEARCH_JSON_DSID = DatastreamId.builder()
        .digitalObject(id)
        .dsid(CustomSearchProperties.DATASTREAM_DSID.name)
        .build();

    var datastreamAsStream = datastreamContentRepository.findById(CUSTOM_SEARCH_JSON_DSID);

    SolrDocument[] solrDocuments;
    try {
      solrDocuments = SolrDocument.from(datastreamAsStream);
    } catch (IOException e) {
      String msg = String.format("Failed to read datastream content %s for datastream %s. Original error: %s", datastreamAsStream.getDescription(), CUSTOM_SEARCH_JSON_DSID, e);
      log.error(msg);
      throw new DatastreamCannotLoadFileException(msg);
    }

    // TODO post batch size of documents? (might be a lot lof solr documents in a single object)

    // currently posting all documents at once
    solrClient.post(SolrGamsCores.CUSTOM_SEARCH_CORE.value,  solrDocuments);
    // commit all documents at once?
    solrClient.commit(SolrGamsCores.CUSTOM_SEARCH_CORE.value);

  }

  /**
   * TODO test
   * TODO jdoc
   *
   * @param projectAbbr project to be indexed to facets database
   * @param id          id of the object to be deleted
   */
  @Override
  public void deleteIndexedObject(String projectAbbr, String id) {
    // TODO hardcoded string
    final String DELETE_QUERY = String.format("%s:%s", "objectId", id);
    solrClient.delete(
        SolrGamsCores.CUSTOM_SEARCH_CORE.value, DELETE_QUERY
    );
    log.info("Deleted indexed object with id: {} from custom search core", id);
  }

  /**
   * Validates and ensures required properties are set on solrDocuments
   * TODO test
   *
   * @param documents documents to be validated and refined
   * @param projectAbbr project abbreviation to be set on each entry
   * @return validated and refined solrDocuments
   */
  public SolrDocument[] validateSolrDocuments(SolrDocument[] documents, String projectAbbr, String objectId) {
    for (var document : documents) {
      // ensure projectAbbr is set
      document.addProperty(CustomSearchProperties.ENTITY_PROJECT_ABBR.name, projectAbbr);
      // ensure object id is set
      document.addProperty(CustomSearchProperties.ENTITY_OBJECT_ID.name, objectId);

      // validate id must start with the projectAbbr
      String entityId = (String) document.getProperty(CustomSearchProperties.ENTITY_ID.name);
      if (!entityId.toLowerCase().startsWith(projectAbbr.toLowerCase())) {
        String msg = String.format("Invalid entity id '%s' in custom search entry for object '%s' in project '%s'. Entity id must start with the project abbreviation.", entityId, objectId, projectAbbr);
        log.error(msg);
        throw new IntegrationDataProcessingException(msg);
      }
    }
    return documents;
  }


  /**
   * Performs a custom search with given parameters.
   * @param fulltext fulltext search string
   * @param projectAbbrs projects to be searched
   * @param tags tags to be filtered by
   * @param pageable pagination information
   * @return custom search response dto
   */
  public CustomSearchResponseDto search(
      String fulltext,
      Set<String> projectAbbrs,
      List<String> tags,
      String startDate,
      String endDate,
      Pageable pageable
  ){

    String baseQuery = CustomSearchSolrQueryBuilder.buildBaseSolrQuery(projectAbbrs, fulltext);

    List<String> filterQueries = new ArrayList<>();

    // TODO rename method to buildTagFilterQueries.
    filterQueries.addAll(
        CustomSearchSolrQueryBuilder.buildFilterQueries(tags)
    );

    // Add date filters
    filterQueries.addAll(
        CustomSearchSolrQueryBuilder.buildDateFilterQueries(startDate, endDate)
    );


    String solrUrl = CustomSearchSolrQueryBuilder.buildSolrUrl(
        SolrGamsCores.CUSTOM_SEARCH_CORE.value,
        baseQuery,
        filterQueries,
        pageable
    );

    log.info("Constructed custom search solr url: {}", solrUrl);

    String response = solrClient.get(solrUrl);

    log.info("Custom search solr response: {}", response);

    return CustomSearchResponseDto.from(response, pageable);

  }

}
