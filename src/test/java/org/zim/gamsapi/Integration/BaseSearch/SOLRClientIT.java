package org.zim.gamsapi.Integration.BaseSearch;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.auditing.AuditingHandler;
import org.zim.gamsapi.IntegrationTest;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SOLRClientIT extends IntegrationTest {

  // disables auditing
  @MockBean
  private AuditingHandler auditingHandler;

  @Autowired
  private BaseSearchService baseSearchService;

  @Autowired
  private SOLRClient solrClient;

  @Test
  public void coreExistsReturnsFalseWhenExpectedCoreDoesNotExist() {
    Assertions.assertThat(
        solrClient.coreExists("test")
    ).isFalse();
  }

  @Test
  public void coreExistsDoesNotThrow(){
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        () -> solrClient.coreExists("test")
    );
  }

}

