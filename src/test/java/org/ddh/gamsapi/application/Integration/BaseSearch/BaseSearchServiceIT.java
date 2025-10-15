package org.ddh.gamsapi.application.Integration.BaseSearch;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.auditing.AuditingHandler;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationDataProcessingException;
import org.ddh.gamsapi.domain.Project.Project;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.ddh.gamsapi.infrastructure.System.configproperties.GAMSDockerDNS;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.TestUtilities.TestProject;


@Slf4j
@Disabled("Disabled because of unclear solr integration test issues.")
public class BaseSearchServiceIT extends BaseSearchIntegrationTest {

  @Autowired
  private BaseSearchService baseSearchService;

  @Autowired
  private IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  private IProjectRepository projectRepository;

  @Autowired
  private GAMSDockerDNS gamsDockerDNS;

//  @Autowired
//  private SOLRClient solrClient;

  private final Project TEST_PROJECT = TestProject.generate();

  // disables auditing
  @MockBean
  private AuditingHandler auditingHandler;
  @Autowired
  private SOLRClient sOLRClient;

  @Nested
  public class IndexObject {


    @Test
    public void tryingToIndexNonExistentObjectShouldThrow(){
      final String NON_EXISTENT_DIGITAL_OBJECT_ID = "DOES_NOT_EXIST";
      Assertions.assertThrows(IntegrationDataProcessingException.class, () -> {
        baseSearchService.indexObject(TEST_PROJECT.getProjectAbbr(), NON_EXISTENT_DIGITAL_OBJECT_ID);
      });
    }

    @Test
    public void ableToIndexTestDigitalObject() {
      // first save the gams project
      projectRepository.save(TEST_PROJECT);

      // then the object
      DigitalObject testObject =  TestDigitalObject.generate();
      digitalObjectRepository.save(testObject);

      // index it
      baseSearchService.indexObject(TEST_PROJECT.getProjectAbbr(), testObject.getId());

      //log.trace("Posting now byte array data to solr core {}", coreName);



//      final String SOLR_SINGLE_CORE_API_ENDPOINT = "/solr";
//
//      String coreName = "test";
//
//      String postUrl = String.format("%s/%s/update/json/docs?commit=true", SOLR_SINGLE_CORE_API_ENDPOINT, coreName);
//      String SOLR_BASE_URL = gamsDockerDNS.getBaseSearchUrl();
//      byte[] data = new byte[0];
//
//      var webClient = WebClient.builder()
//          .baseUrl(gamsDockerDNS.getBaseSearchUrl())
//          .build();
//
//      try {
//        webClient.post()
//            .uri(postUrl)
//            .contentType(MediaType.APPLICATION_JSON)
//            .bodyValue(data)
//            .retrieve()
//            .toBodilessEntity()
//            .block();
//        // If the request was successful, you can handle the response here
//        log.info("Successfully posted data to solr core {}", coreName);
//
//      } catch (WebClientResponseException e) {
//        // This exception contains the response body from the server
//        String errorResponseBody = e.getResponseBodyAsString();
//        String msg = String.format("Failed to post data to solr core %s. Via baseUrl %s and endpoint %s. Status: %s. Error response from solr: %s",
//            coreName, SOLR_BASE_URL, postUrl, e.getStatusCode(), errorResponseBody);
//        log.error(msg);
//        throw new IntegrationServiceException(msg);
//      } catch (WebClientException e) {
//        String msg = String.format("Failed to post data to solr core %s. Via baseUrl %s and endpoint %s and body %s Cause: %s. Original error: %s", coreName, SOLR_BASE_URL, postUrl, data, e.getMessage(), e);
//        log.error(msg);
//        throw new IntegrationServiceException(msg);
//      }
//
    }

  }


}
