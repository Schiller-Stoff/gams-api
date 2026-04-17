package org.ddh.gamsapi.infrastructure.System.configproperties;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gams.environment")
@Getter
@Setter
public class GamsEnvironmentProperties {

  /**
   * Root of GAMS files (datastreams) to be stored.
   */
  @NotNull
  private boolean allowDirectModifications;
}
