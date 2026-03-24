package org.ddh.gamsapi.infrastructure.System.configproperties;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Handles configuration properties related to the semantic search service.
 * Defined in the application.yml file.
 */
@Configuration
@ConfigurationProperties(prefix = "gams.services.semantic-search")
@Getter
@Setter
public class SemanticSearchProperties {

  /**
   * Access token for QLever write operations (SPARQL UPDATE).
   * QLever requires this as a query parameter {@code ?access-token=...} on all
   * state-changing requests. Configured via {@code gams.qlever.access-token}.
   */
  private String accessToken;

  /**
   * Path to the shared volume where bulk export files are stored.
   */
  private String exportPath;


}
