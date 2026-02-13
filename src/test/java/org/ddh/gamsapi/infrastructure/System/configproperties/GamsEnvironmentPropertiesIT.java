package org.ddh.gamsapi.infrastructure.System.configproperties;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GamsEnvironmentPropertiesIT extends IntegrationTest {

  @Autowired
  private GamsEnvironmentProperties gamsEnvironmentProperties;

  @Test
  public void hasExpectedDefaultValue(){

    Assertions.assertThat(
        gamsEnvironmentProperties.isAllowDirectModifications()
    ).isTrue();

  }

}
