package org.zim.gamsapi.System.configproperties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Contains properties related to the docker / networking environment of GAMS, like
 * different DNS entries.
 * Spring injects properties from application.yml as defined
 * in the prefix-parameter from @ConfigurationProperties
 * see: <a href="https://www.baeldung.com/configuration-properties-in-spring-boot">...</a>
 */
@Configuration
@ConfigurationProperties(prefix = "gams.docker")
@Getter
@Setter
public class GAMSDockerDNS {

  /**
   * Docker DNS of relationalDB
   */
  @NotBlank
  private String relationalDb;

  @NotBlank
  private String baseSearchUrl;

  @NotBlank
  private String triplestoreUrl;

  @NotBlank
  private String elasticsearchUrl;

  @NotBlank
  private String keycloakUrl;
}
