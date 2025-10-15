package org.ddh.gamsapi.infrastructure.System.configproperties;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Handles configuration properties related to CORS (Cross-Origin Resource Sharing)
 * for GAMS API.
 */
@Configuration
@ConfigurationProperties(prefix = "gams.cors")
@Getter
@Setter
public class GAMSCorsProperties {

  private String[] allowedOrigins = {};

  private String[] allowedOriginPatterns = {};

  @NotEmpty
  private long maxAge;

}
