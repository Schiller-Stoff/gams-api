package org.zim.gamsapi.Integration.BaseSearch;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
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
        solrClient.coreExists("RANDOM_CORE_NAME")
    ).isFalse();
  }

  @Test
  public void coreExistsDoesNotThrow(){
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        () -> solrClient.coreExists("test")
    );
  }

  @Test
  public void createsExpectedCore() {
    final String TEST_CORE_NAME = "DOES_NOT_EXIST";
    Assertions.assertThat(
        solrClient.coreExists(TEST_CORE_NAME)
    ).isFalse();
    solrClient.createCore(TEST_CORE_NAME);
    Assertions.assertThat(
        solrClient.coreExists(TEST_CORE_NAME)
    ).isTrue();
    // TODO danger -> core still exists after test!
  }

  @Test
  public void deletesExpectedCore(){
    final String TEST_CORE_NAME = "bla";
    Assertions.assertThat(
        solrClient.coreExists(TEST_CORE_NAME)
    ).isFalse();
    solrClient.createCore(TEST_CORE_NAME);
    Assertions.assertThat(
        solrClient.coreExists(TEST_CORE_NAME)
    ).isTrue();
    solrClient.removeCore(TEST_CORE_NAME);
    Assertions.assertThat(
        solrClient.coreExists(TEST_CORE_NAME)
    ).isFalse();
  }

  @Test
  public void verifyThatTestCoreExists(){
    Assertions.assertThat(
        solrClient.coreExists(
            IntegrationTest.SOLR_TEST_CORE
        )
    ).isTrue();
  }

  @Test
  @Disabled
  public void wipeCoreWorksAsExpected(){

    final String TEST_CORE_NAME = "DOES_NOT_EXIST";
    // this core should not exist
    Assertions.assertThat(
        solrClient.coreExists(TEST_CORE_NAME)
    ).isFalse();
    solrClient.createCore(TEST_CORE_NAME);
    // created core exists now
    Assertions.assertThat(
        solrClient.coreExists(TEST_CORE_NAME)
    ).isTrue();

    // core should be empty now
    Assertions.assertThat(
        solrClient.checkCoreIsEmpty(TEST_CORE_NAME)
    ).isTrue();

    final BaseSearch baseSearch = new BaseSearch();
    baseSearch.addProperty("id", "123");
    solrClient.post(TEST_CORE_NAME, baseSearch);

    // this core should be filled now with data
    Assertions.assertThat(
        solrClient.checkCoreIsEmpty(TEST_CORE_NAME)
    ).isFalse();

  }

  @Test
  @Disabled
  public void _enableToKeepSolrRunning() throws InterruptedException {
    Thread.sleep(10000000);
  }

}

