package org.zim.gamsapi.Integration.BaseSearch;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.auditing.AuditingHandler;
import org.zim.gamsapi.IntegrationTest;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("Deactivated integration testing for SOLR because of unclear issues with the solr test-container.")
public class SOLRClientIT extends BaseSearchIntegrationTest {

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
    solrClient.removeCore(TEST_CORE_NAME);
    Assertions.assertThat(
        solrClient.coreExists(TEST_CORE_NAME)
    ).isFalse();
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
            BaseSearchIntegrationTest.SOLR_TEST_CORE
        )
    ).isTrue();
  }

  @Test
  public void checkCoreIsEmptyWorksAsExpected(){

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

    solrClient.removeCore(TEST_CORE_NAME);
    // core should not exist now
    Assertions.assertThat(
        solrClient.coreExists(TEST_CORE_NAME)
    ).isFalse();
  }

  @Test
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

    // wipe core
    solrClient.wipeCore(TEST_CORE_NAME);
    // core should be empty again
    Assertions.assertThat(
        solrClient.checkCoreIsEmpty(TEST_CORE_NAME)
    ).isTrue();
    // core should still exist
    Assertions.assertThat(
        solrClient.coreExists(TEST_CORE_NAME)
    ).isTrue();

    // remove core
    solrClient.removeCore(TEST_CORE_NAME);
    // core should not exist now
    Assertions.assertThat(
        solrClient.coreExists(TEST_CORE_NAME)
    ).isFalse();

  }


  @Nested
  public class Post {

    @Test
    public void postingDataToSolrFillsTestedCore(){

      final String TEST_CORE_NAME = "DOES_NOT_EXIST";
      Assertions.assertThat(
          solrClient.coreExists(TEST_CORE_NAME)
      ).isFalse();

      solrClient.createCore(TEST_CORE_NAME);
      Assertions.assertThat(
          solrClient.checkCoreIsEmpty(TEST_CORE_NAME)
      ).isTrue();

      final BaseSearch baseSearch = new BaseSearch();
      baseSearch.addProperty("id", "123");
      solrClient.post(TEST_CORE_NAME, baseSearch);

      Assertions.assertThat(solrClient.checkCoreIsEmpty(TEST_CORE_NAME)).isFalse();

      // clean up
      solrClient.removeCore(TEST_CORE_NAME);
    }

  }


  @Test
  @Disabled
  public void _enableToKeepSolrRunning() throws InterruptedException {
    Thread.sleep(10000000);
  }

}

