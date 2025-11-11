package org.ddh.gamsapi.application.Integration.Common.utils.solr;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Solr client behavior.
 *
 * Use application.properties or application-test.properties to control:
 * - Whether posts automatically commit
 * - Batch sizes for bulk operations
 * - Commit intervals
 */
@Configuration
@ConfigurationProperties(prefix = "gams.solr")
@Getter
@Setter
public class SolrClientProperties {

  /**
   * Whether to automatically commit after every POST operation.
   *
   * Default: false (rely on Solr's autoCommit)
   *
   * Set to true in tests for immediate visibility without explicit commits.
   * Set to false in production for better performance.
   */
  private boolean autoCommit = false;

  /**
   * Default batch size for bulk indexing operations.
   * Recommended: 500-2000
   */
  private int batchSize = 500;

  /**
   * How often to commit during bulk operations (every N batches).
   * Example: commitInterval=10 means commit every 5000 documents (10 batches × 500 docs)
   */
  private int commitInterval = 10;

}
