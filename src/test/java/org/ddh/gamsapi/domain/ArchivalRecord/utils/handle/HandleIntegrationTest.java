package org.ddh.gamsapi.domain.ArchivalRecord.utils.handle;

import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.IHandleClient;
import org.ddh.gamsapi.infrastructure.System.configproperties.HandleServerProperties;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class HandleIntegrationTest extends IntegrationTest {


  @Autowired
  public IHandleClient handleClient;

  @Autowired
  HandleServerProperties handleServerProperties;

  /**
   * Checks if the handle server is available before each handle test.
   * If not skips the related tests.
   */
  @BeforeEach
  void assumeHandleServerIsReachable(){

    boolean handleServerNotReachableExceptionThrown = false;

    try {
      handleClient.verifyReachable();
    } catch (HandleServerNotReachableException e) {
      handleServerNotReachableExceptionThrown = true;
    }

    Assumptions.assumeFalse(
        handleServerNotReachableExceptionThrown,
        "Skipping handle server tests because the handle server is not reachable in the testing environment. Expected url: " + handleServerProperties.getBaseUrl()
    );

  }


}
