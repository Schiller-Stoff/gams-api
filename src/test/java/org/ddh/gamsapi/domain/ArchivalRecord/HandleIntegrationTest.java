package org.ddh.gamsapi.domain.ArchivalRecord;

import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestArchivalRecord;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.handle.Handle;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.handle.HandleNotRegisteredException;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.handle.HandleServerNotReachableException;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.handle.IHandleClient;
import org.ddh.gamsapi.infrastructure.System.configproperties.HandleServerProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class HandleIntegrationTest extends IntegrationTest {

  @Autowired
  public IHandleClient handleClient;

  @Autowired
  HandleServerProperties handleServerProperties;

  private boolean handleServerReachable;

  /**
   * Checks if the handle server is available before each handle test.
   * If not skips the related tests.
   */
  @BeforeEach
  void assumeHandleServerIsReachable() {
    try {
      handleClient.verifyReachable();
      handleServerReachable = true;
    } catch (HandleServerNotReachableException e) {
      handleServerReachable = false;
    }
    Assumptions.assumeTrue(handleServerReachable,
        "Skipping handle server tests: handle server not reachable at " + handleServerProperties.getBaseUrl());
  }

  /**
   * Cleanup if the handle server is reachable.
   */
  @AfterEach
  void makeSureTestHandleDoesNotExist() {
    if (!handleServerReachable) {
      return; // test was aborted in @BeforeEach – nothing to clean up
    }
    try {
      handleClient.delete(Handle.parse(TestArchivalRecord.PID).toHdlUri());
    } catch (HandleNotRegisteredException _) {
      // test handle was not registered
    }
  }
}
