package org.zim.gamsapi.System.configproperties;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Handles configuration properties related to the storage layer of GAMS.
 * Defined in the application.yml file.
 */
@Configuration
@ConfigurationProperties(prefix = "gams.storage")
@Getter
@Setter
public class GAMSStorageProperties {

  /**
   * Root of GAMS files (datastreams) to be stored.
   */
  @NotBlank
  private String rootPath;

}
