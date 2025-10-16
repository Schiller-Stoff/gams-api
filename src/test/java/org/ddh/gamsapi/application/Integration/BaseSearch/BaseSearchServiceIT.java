package org.ddh.gamsapi.application.Integration.BaseSearch;

import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationDataProcessingException;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationServiceException;
import org.ddh.gamsapi.infrastructure.System.configproperties.GAMSDockerDNS;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;


@Slf4j
public class BaseSearchServiceIT extends BaseSearchIntegrationTest {

  @Autowired
  private BaseSearchService baseSearchService;

  @Autowired
  private GAMSDockerDNS gamsDockerDNS;

  // disables auditing
  @MockitoBean
  private AuditingHandler auditingHandler;

  @Autowired
  private SOLRClient sOLRClient;

  @Autowired
  private TestDataBuilder testDataBuilder;

  private TestDataSet testDataSet;

  @BeforeEach
  public void setup() {
    testDataSet = testDataBuilder.buildTestDataSet();
  }

  @Nested
  public class IndexObject {

    @Test
    public void tryingToIndexNonExistentObjectShouldThrow(){
      final String NON_EXISTENT_DIGITAL_OBJECT_ID = "DOES_NOT_EXIST";
      Assertions.assertThrows(IntegrationDataProcessingException.class, () -> {
        baseSearchService.indexObject(testDataSet.project().getProjectAbbr(), NON_EXISTENT_DIGITAL_OBJECT_ID);
      });
    }

    @Test
    public void indexObjectDoesNotThrow() {

      // index object
      baseSearchService.indexObject(
          testDataSet.project().getProjectAbbr(), testDataSet.digitalObject().getId()
      );

      String coreName = "test";
      log.trace("Posting now byte array data to solr core {}", coreName);

      final String SOLR_SINGLE_CORE_API_ENDPOINT = "/solr";


      String postUrl = String.format("%s/%s/update/json/docs?commit=true", SOLR_SINGLE_CORE_API_ENDPOINT, coreName);
      String SOLR_BASE_URL = gamsDockerDNS.getBaseSearchUrl();
      byte[] data = new byte[0];

      var webClient = WebClient.builder()
          .baseUrl(gamsDockerDNS.getBaseSearchUrl())
          .build();

      try {
        webClient.post()
            .uri(postUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(data)
            .retrieve()
            .toBodilessEntity()
            .block();
        // If the request was successful, you can handle the response here
        log.info("Successfully posted data to solr core {}", coreName);

      } catch (WebClientResponseException e) {
        // This exception contains the response body from the server
        String errorResponseBody = e.getResponseBodyAsString();
        String msg = String.format("Failed to post data to solr core %s. Via baseUrl %s and endpoint %s. Status: %s. Error response from solr: %s",
            coreName, SOLR_BASE_URL, postUrl, e.getStatusCode(), errorResponseBody);
        log.error(msg);
        throw new IntegrationServiceException(msg);
      } catch (WebClientException e) {
        String msg = String.format("Failed to post data to solr core %s. Via baseUrl %s and endpoint %s and body %s Cause: %s. Original error: %s", coreName, SOLR_BASE_URL, postUrl, data, e.getMessage(), e);
        log.error(msg);
        throw new IntegrationServiceException(msg);
      }

    }

  }


}
