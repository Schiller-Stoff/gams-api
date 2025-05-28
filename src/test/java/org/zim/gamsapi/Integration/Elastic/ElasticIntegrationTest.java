package org.zim.gamsapi.Integration.Elastic;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.zim.gamsapi.IntegrationTest;

/**
 * Superclass for integration tests that require Elasticsearch
 * additionally to the base integration test setup (postgres / auth / etc.).
 */
@Slf4j
public class ElasticIntegrationTest extends IntegrationTest {

  @Autowired
  ElasticDigitalObjectRepository elasticDigitalObjectRepository;

  @Autowired
  ElasticsearchOperations elasticsearchOperations;

  @AfterEach
  public void tearDown() throws InterruptedException {
    elasticDigitalObjectRepository.deleteAll();
    // seems necessary to create and recreate the elastic index for testing
    // (otherwise there is a OptimisticTransactionLock exception)
    IndexCoordinates indexCoordinates = IndexCoordinates.of(ElasticDigitalObject.INDEX_NAME);
    elasticsearchOperations.indexOps(indexCoordinates).delete();
    elasticsearchOperations.indexOps(indexCoordinates).create();
  }

}
