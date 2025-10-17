package org.ddh.gamsapi.application.Integration.BaseSearch;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SOLRClientIT extends BaseSearchIntegrationTest {

  // disables auditing
  @MockitoBean
  private AuditingHandler auditingHandler;

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
        () -> solrClient.coreExists(GamsSolrCores.TEST_CORE.value)
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
            GamsSolrCores.TEST_CORE.value
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

  @Nested
  public class GET {

    @Test
    public void getReturnsExpectedData(){
      String TEST_SOLR_DOCUMENT_PROPERTY_NAME = "id";
      String TEST_SOLR_DOCUMENT_PROPERTY_VALUE = "123";

      final BaseSearch baseSearch = new BaseSearch();
      baseSearch.addProperty(TEST_SOLR_DOCUMENT_PROPERTY_NAME, TEST_SOLR_DOCUMENT_PROPERTY_VALUE);
      solrClient.post(GamsSolrCores.TEST_CORE.value, baseSearch);

      String response = solrClient.retrieveSolrDocumentByProperty(GamsSolrCores.TEST_CORE.value, "id", "123");
      String expectedSubstring = String.format("\"%s\":\"%s\"", TEST_SOLR_DOCUMENT_PROPERTY_NAME, TEST_SOLR_DOCUMENT_PROPERTY_VALUE);
      Assertions.assertThat(response).contains(expectedSubstring);

    }

    @Test
    public void queryReturnsExpectedData(){
      String TEST_SOLR_DOCUMENT_PROPERTY_NAME = "id";
      String TEST_SOLR_DOCUMENT_PROPERTY_VALUE = "1234";

      final BaseSearch baseSearch = new BaseSearch();
      baseSearch.addProperty(TEST_SOLR_DOCUMENT_PROPERTY_NAME, TEST_SOLR_DOCUMENT_PROPERTY_VALUE);
      solrClient.post(GamsSolrCores.TEST_CORE.value, baseSearch);

      String solrQuery = String.format("%s:%s", TEST_SOLR_DOCUMENT_PROPERTY_NAME, TEST_SOLR_DOCUMENT_PROPERTY_VALUE);
      String response = solrClient.query(GamsSolrCores.TEST_CORE.value, solrQuery);
      String expectedSubstring = String.format("\"%s\":\"%s\"", TEST_SOLR_DOCUMENT_PROPERTY_NAME, TEST_SOLR_DOCUMENT_PROPERTY_VALUE);
      Assertions.assertThat(response).contains(expectedSubstring);

    }

  }


  @Test
  @Disabled
  public void _enableToKeepSolrRunning() throws InterruptedException {
    Thread.sleep(10000000);
  }

}

