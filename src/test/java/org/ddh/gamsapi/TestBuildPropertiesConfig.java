package org.ddh.gamsapi;

import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import java.util.Properties;

/**
 * Required build-info.properties is only created during mvn package
 * This class provides a mock during testing
 */
@Configuration
public class TestBuildPropertiesConfig {

  @Bean
  public BuildProperties buildProperties() {
    Properties properties = new Properties();
    properties.put("artifact", "gams-api");
    properties.put("name", "gams-api");
    properties.put("version", "0.0.1-SNAPSHOT-TEST");
    properties.put("group", "org.ddh");
    properties.put("time", "2025-01-01T00:00:00Z");
    return new BuildProperties(properties);
  }
}