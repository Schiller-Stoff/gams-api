package org.ddh.gamsapi.application.Integration.CustomSearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearch;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearchProperties;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationDataProcessingException;
import org.ddh.gamsapi.application.Integration.Common.interfaces.IIntegrationService;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrClient;
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
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service for custom search integration.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomSearchService implements IIntegrationService {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;
  private final IDatastreamContentRepository datastreamContentRepository;
  private final IDublinCoreEntryRepository dublinCoreEntryRepository;


  private final SolrClient solrClient;


  @Override
  public void indexObjects(String projectAbbr) {

    // https://claude.ai/chat/712f87c4-aa48-48c1-bf63-b3c5a91f629d

    log.info("*** {}: Starting project indexing for: {}", this.getClass().getName(), projectAbbr);

    var page = datastreamRepository.findAllByDsidAndProject(
        //  TODO hardcoded string
        "CUSTOM_SEARCH.json",
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

    if(page.getTotalElements() == 0){
      log.info("No digital objects with FULLTEXT_INDEX.json datastream found for project: {}", projectAbbr);
      return;
    }

    int batchCount = 0;
    int objectsProcessed = 0;

    // TODO this should be configurable?
    final int DEFAULT_BATCH_SIZE = 500;


    // TODO BaseSearch object seems off here - should we work with SolrDocument instead?
    List<BaseSearch> currentBatch = new ArrayList<>(DEFAULT_BATCH_SIZE);
    for (IDatastreamIndexingView datastreamIndexingView : page.getContent()) {
      objectsProcessed++;

      DatastreamId datastreamId = DatastreamId.builder()
          .dsid(datastreamIndexingView.getDsid())
          .digitalObject(datastreamIndexingView.getDigitalObject().getId())
          .build();

      // load datastream content
      InputStreamResource resource = datastreamContentRepository.findById(datastreamId);

      // parse datastream content as BaseSearch array
      // TODO check if working on BaseSearch entities is correct / maybe use SolrDocument here?
      BaseSearch[] baseSearch;
      try {
        baseSearch = BaseSearch.from(resource);
        baseSearch = refineBaseSearchEntries(baseSearch, projectAbbr, datastreamId.getDigitalObject());
      } catch (IOException e) {
        String msg = String.format("Failed to read datastream content %s for datastream %s. Original error: %s", resource.getDescription(), datastreamId, e);
        log.error(msg);
        // TODO better / different exception
        throw new DatastreamCannotLoadFileException(msg);
      }

      // TODO validate baseSearch entries?
      // TODO objectId + objectProjectAbbr - should be added by the gams-api (should be available no matter what)

      //02. add to batch
      // TODO fix - inefficient array to list conversion - and back
      currentBatch.addAll(Arrays.asList(baseSearch));

      //03. After reaching batch size, post to solr + clear batch (with commit)
      // Flush batch if it's getting full (but don't commit yet)
      if (currentBatch.size() >= DEFAULT_BATCH_SIZE) {
        log.info("Processed {} BaseSearch objects from datastream {} - current batch: {}", currentBatch.size(), datastreamId, currentBatch);
        solrClient.post(SolrGamsCores.CUSTOM_SEARCH_CORE.value, currentBatch.toArray(new BaseSearch[0]));
        currentBatch.clear();
        batchCount++;
        log.info("Processed batch {} for fulltext core ({} objects processed so far)", batchCount, objectsProcessed);
      }

    }

    // Post remaining documents in batch (e.g. when batch size not reached at the end / or never reached)
    if (!currentBatch.isEmpty()) {
      solrClient.post(SolrGamsCores.CUSTOM_SEARCH_CORE.value, currentBatch.toArray(new BaseSearch[0]));
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
   * @param projectAbbr Project abbreviation
   */
  @Override
  public void deleteIndexedObjects(String projectAbbr) {
    final String DELETE_QUERY = String.format("%s:%s", BaseSearchProperties.PROJECT.name, projectAbbr);
    solrClient.delete(
        SolrGamsCores.CUSTOM_SEARCH_CORE.value,
        DELETE_QUERY
    );
    log.info("Deled all indexed objects for project: {} from custom search core", projectAbbr);
  }

  /**
   * TODO jdoc
   * TODO test
   * @param projectAbbr project to be indexed
   * @param id id of the object to be indexed
   */
  @Override
  public void indexObject(String projectAbbr, String id) {

    // TODO implement if needed
    throw new UnsupportedOperationException("indexObject not implemented in CustomSearchService yet.");

  }

  /**
   * TODO test
   * TODO jdoc
   * @param projectAbbr project to be indexed to facets database
   * @param id id of the object to be deleted
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
   * Validates and ensures required properties are set on BaseSearch entries.
   * TODO test
   * @param entries BaseSearch entries to be validated and refined
   * @param projectAbbr project abbreviation to be set on each entry
   * @return validated and refined BaseSearch entries
   */
  public BaseSearch[] refineBaseSearchEntries(BaseSearch[] entries, String projectAbbr, String objectId) {
    for (var entry : entries) {
      // ensure projectAbbr is set
      entry.addProperty(CustomSearchEntityProperties.ENTITY_PROJECT_ABBR.name, projectAbbr);
      // ensure object id is set
      entry.addProperty(CustomSearchEntityProperties.ENTITY_OBJECT_ID.name, objectId);
    }
    return entries;
  }

}
