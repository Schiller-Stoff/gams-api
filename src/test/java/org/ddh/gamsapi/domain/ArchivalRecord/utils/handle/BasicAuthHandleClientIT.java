package org.ddh.gamsapi.domain.ArchivalRecord.utils.handle;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.IHandleClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;

class BasicAuthHandleClientIT extends HandleIntegrationTest {

  @Autowired
  public IHandleClient handleClient;

  @Test
  void testCreateHandle(){
    String pid = handleClient.generate();
    handleClient.register(pid, URI.create("https://google.at"));

    Assertions.assertThat(
        handleClient.exists(pid)
    ).isTrue();

  }

}
