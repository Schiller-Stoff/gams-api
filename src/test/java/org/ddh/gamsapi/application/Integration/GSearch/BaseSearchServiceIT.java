package org.ddh.gamsapi.application.Integration.GSearch;

import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.TestUtilities.TestBag;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.application.Ingest.Ingest;
import org.ddh.gamsapi.application.Ingest.interfaces.IIngestService;
import org.ddh.gamsapi.application.Ingest.utils.ZipUtils;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrGamsCores;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationDataProcessingException;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationServiceException;
import org.ddh.gamsapi.application.Integration.SolrIntegrationTest;
import org.ddh.gamsapi.domain.Project.ProjectBuilder;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
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

import java.io.File;
import java.io.IOException;


@Slf4j
public class BaseSearchServiceIT extends SolrIntegrationTest {

  @Autowired
  private BaseSearchService baseSearchService;

  @Autowired
  private IIngestService ingestService;

  @Autowired
  private IProjectRepository projectRepository;

  @Autowired
  private GAMSDockerDNS gamsDockerDNS;

  // disables auditing
  @MockitoBean
  private AuditingHandler auditingHandler;

  File bagFile;

  @BeforeEach
  public void setup() throws IOException {
    bagFile = TestBag.loadFile();
    projectRepository.save(ProjectBuilder.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build());

    // ingest the bag
    byte[] zippedBag = ZipUtils.zipDir(bagFile);
    Ingest ingest = new Ingest();
    ingest.setZippedBagItFolder(zippedBag);
    ingest.setProjectAbbr(TestProject.PROJECT_ABBR.getValue());
    ingestService.ingest(ingest);
  }


  @Nested
  public class IndexObject {

    @Test
    public void tryingToIndexNonExistentObjectShouldThrow(){
      final String NON_EXISTENT_DIGITAL_OBJECT_ID = "DOES_NOT_EXIST";
      Assertions.assertThrows(IntegrationDataProcessingException.class, () -> {
        baseSearchService.indexObject(TestProject.PROJECT_ABBR.getValue(), NON_EXISTENT_DIGITAL_OBJECT_ID);
      });
    }

    @Test
    public void indexObjectDoesNotThrow() {

      // index object
      baseSearchService.indexObject(
          TestProject.PROJECT_ABBR.getValue(), TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
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

    @Test
    public void findsExpectedObjectIdJsonEntry(){
      // index object
      baseSearchService.indexObject(
          TestProject.PROJECT_ABBR.getValue(), TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
      );
      String response = solrClient.retrieveSolrDocumentByProperty(
          SolrGamsCores.GAMS_CORE.value, "id", TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
      );

      // solr also returns the initial query info, so we just check that the id is contained in the response
      org.assertj.core.api.Assertions.assertThat(response)
          .isNotNull()
          .contains("\"id\":\""+ TestDigitalObject.DIGITAL_OBJECT_ID.getValue());
      ;

    }

    @Test
    public void ableToFindDocumentsBasedOnProjectAbbreviations(){
      // index object
      baseSearchService.indexObject(
          TestProject.PROJECT_ABBR.getValue(), TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
      );
      String response = solrClient.retrieveSolrDocumentByProperty(
          SolrGamsCores.GAMS_CORE.value, BaseSearchProperties.PROJECT.name, TestProject.PROJECT_ABBR.getValue()
      );

      System.out.println("*** Response: " + response);

      // solr also returns the initial query info, so we just check that the id is contained in the response
      org.assertj.core.api.Assertions.assertThat(response)
          .isNotNull()
          .contains("\"id\":\""+ TestDigitalObject.DIGITAL_OBJECT_ID.getValue());
      ;
    }

    @Test
    public void returnsExpectedDcFieldNamesInResponse(){

      // index object
      baseSearchService.indexObject(
          TestProject.PROJECT_ABBR.getValue(), TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
      );
      String response = solrClient.retrieveSolrDocumentByProperty(
          SolrGamsCores.GAMS_CORE.value, "id", TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
      );

      // solr also returns the initial query info, so we just check that the dc_title is contained in the response
      org.assertj.core.api.Assertions.assertThat(response)
          .isNotNull()
          .contains(
              "dc.title",
              "dc.creator",
              "dc.subject",
              "dc.description",
              "dc.publisher",
              "dc.contributor",
              "dc.date",
              "dc.type",
              "dc.format",
              "dc.identifier",
              "dc.source",
              "dc.language",
              "dc.relation",
              "dc.coverage",
              "dc.rights"
          )
      ;

    }

  }
}
