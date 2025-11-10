package org.ddh.gamsapi.application.Common.utils.solr;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.application.Integration.SolrIntegrationTest;
import org.ddh.gamsapi.application.Integration.GSearch.GSearchProperties;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrClient;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrDocument;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrGamsCores;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Set;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SolrClientIT extends SolrIntegrationTest {

  // disables auditing
  @MockitoBean
  private AuditingHandler auditingHandler;

  @Autowired
  private SolrClient solrClient;

  @Test
  public void coreExistsReturnsFalseWhenExpectedCoreDoesNotExist() {
    Assertions.assertThat(
        solrClient.coreExists("RANDOM_CORE_NAME")
    ).isFalse();
  }

  @Test
  public void coreExistsDoesNotThrow(){
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        () -> solrClient.coreExists(SolrGamsCores.TEST_CORE.value)
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
            SolrGamsCores.TEST_CORE.value
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

    final SolrDocument solrDocument = new SolrDocument();
    solrDocument.addProperty("id", "123");
    solrClient.post(TEST_CORE_NAME, solrDocument);

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

    final SolrDocument solrDocument = new SolrDocument();
    solrDocument.addProperty("id", "123");
    solrClient.post(TEST_CORE_NAME, solrDocument);

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

      final SolrDocument solrDocument = new SolrDocument();
      solrDocument.addProperty("id", "123");
      solrClient.post(TEST_CORE_NAME, solrDocument);

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

      final SolrDocument solrDocument = new SolrDocument();
      solrDocument.addProperty(TEST_SOLR_DOCUMENT_PROPERTY_NAME, TEST_SOLR_DOCUMENT_PROPERTY_VALUE);
      solrClient.post(SolrGamsCores.TEST_CORE.value, solrDocument);

      String response = solrClient.retrieveSolrDocumentByProperty(SolrGamsCores.TEST_CORE.value, "id", "123");
      String expectedSubstring = String.format("\"%s\":\"%s\"", TEST_SOLR_DOCUMENT_PROPERTY_NAME, TEST_SOLR_DOCUMENT_PROPERTY_VALUE);
      Assertions.assertThat(response).contains(expectedSubstring);

    }

    @Test
    public void queryReturnsExpectedData(){
      String TEST_SOLR_DOCUMENT_PROPERTY_NAME = "id";
      String TEST_SOLR_DOCUMENT_PROPERTY_VALUE = "1234";

      final SolrDocument solrDocument = new SolrDocument();
      solrDocument.addProperty(TEST_SOLR_DOCUMENT_PROPERTY_NAME, TEST_SOLR_DOCUMENT_PROPERTY_VALUE);
      solrClient.post(SolrGamsCores.TEST_CORE.value, solrDocument);

      String solrQuery = String.format("%s:%s", TEST_SOLR_DOCUMENT_PROPERTY_NAME, TEST_SOLR_DOCUMENT_PROPERTY_VALUE);
      String response = solrClient.query(SolrGamsCores.TEST_CORE.value, solrQuery);
      String expectedSubstring = String.format("\"%s\":\"%s\"", TEST_SOLR_DOCUMENT_PROPERTY_NAME, TEST_SOLR_DOCUMENT_PROPERTY_VALUE);
      Assertions.assertThat(response).contains(expectedSubstring);

    }

    @Test
    public void getReturnsExpectedSolrData(){
      String TEST_SOLR_DOCUMENT_PROPERTY_NAME = "id";
      String TEST_SOLR_DOCUMENT_PROPERTY_VALUE = "1234";

      final SolrDocument solrDocument = new SolrDocument();
      solrDocument.addProperty(TEST_SOLR_DOCUMENT_PROPERTY_NAME, TEST_SOLR_DOCUMENT_PROPERTY_VALUE);
      solrClient.post(SolrGamsCores.TEST_CORE.value, solrDocument);

      String url = String.format("/solr/%s/select?q=*:*", SolrGamsCores.TEST_CORE.value);
      String response = solrClient.get(url);
      System.out.println("*** Response: " + response);
      Assertions.assertThat(response)
          .isNotNull()
          .isNotEmpty()
          .contains(TEST_SOLR_DOCUMENT_PROPERTY_NAME, TEST_SOLR_DOCUMENT_PROPERTY_VALUE)
      ;

    }

  }

  @Nested
  public class CountProjectDocuments {

    @Test
    public void returnsExpectedCount(){
      String TEST_SOLR_DOCUMENT_PROPERTY_NAME = GSearchProperties.PROJECT.name;
      String TEST_SOLR_DOCUMENT_PROPERTY_VALUE = TestProject.PROJECT_ABBR.getValue();

      final SolrDocument solrDocument = new SolrDocument();
      solrDocument.addProperty(TEST_SOLR_DOCUMENT_PROPERTY_NAME, TEST_SOLR_DOCUMENT_PROPERTY_VALUE);
      solrDocument.addProperty(GSearchProperties.OBJECT_ID.name, TestDigitalObject.DIGITAL_OBJECT_ID.getValue());
      solrClient.post(SolrGamsCores.TEST_CORE.value, solrDocument);

      int documentCount = solrClient.countProjectDocuments(
          SolrGamsCores.TEST_CORE.value,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );
      Assertions.assertThat(documentCount)
          .isGreaterThan(0);
    }

    @Test
    public void returnsZeroWhenNoDocumentsExist(){
      int documentCount = solrClient.countProjectDocuments(
          SolrGamsCores.TEST_CORE.value,
          Set.of("NON_EXISTENT_PROJECT_ABBR")
      );
      Assertions.assertThat(documentCount)
          .isEqualTo(0);
    }

    @Test
    public void returnsCountWhenProjectSetIsEmpty(){
      String TEST_SOLR_DOCUMENT_PROPERTY_NAME = GSearchProperties.PROJECT.name;
      String TEST_SOLR_DOCUMENT_PROPERTY_VALUE = TestProject.PROJECT_ABBR.getValue();

      final SolrDocument solrDocument = new SolrDocument();
      solrDocument.addProperty(TEST_SOLR_DOCUMENT_PROPERTY_NAME, TEST_SOLR_DOCUMENT_PROPERTY_VALUE);
      solrDocument.addProperty(GSearchProperties.OBJECT_ID.name, TestDigitalObject.DIGITAL_OBJECT_ID.getValue());
      solrClient.post(SolrGamsCores.TEST_CORE.value, solrDocument);

      int documentCount = solrClient.countProjectDocuments(
          SolrGamsCores.TEST_CORE.value,
          Set.of()
      );
      Assertions.assertThat(documentCount)
          .isGreaterThan(0);
    }

  }


  @Test
  @Disabled
  public void _enableToKeepSolrRunning() throws InterruptedException {
    Thread.sleep(10000000);
  }

}

