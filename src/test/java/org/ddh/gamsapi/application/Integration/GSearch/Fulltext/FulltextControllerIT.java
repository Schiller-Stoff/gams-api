package org.ddh.gamsapi.application.Integration.GSearch.Fulltext;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestBag;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.TestUtilities.TestDublinCoreEntry;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.application.Ingest.interfaces.IIngestService;
import org.ddh.gamsapi.application.Ingest.utils.ZipUtils;
import org.ddh.gamsapi.application.Integration.GSearch.ApiSearchService;
import org.ddh.gamsapi.application.Integration.SolrIntegrationTest;
import org.ddh.gamsapi.domain.Project.ProjectBuilder;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.ddh.gamsapi.infrastructure.System.security.IUserPrincipalAuditorMapping;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@AutoConfigureMockMvc(addFilters = false)
public class FulltextControllerIT extends SolrIntegrationTest {

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ApiSearchService apiSearchService;

  @Autowired
  private IIngestService ingestService;

  @Autowired
  private IProjectRepository projectRepository;

  /**
   * Classes need to mock authenticated users when changing datastreams
   */
  @MockitoBean
  private AuditingHandler auditingHandler;
  @MockitoBean
  private IUserPrincipalAuditorMapping userPrincipalAuditorMapping;

  File bagFile;

  @BeforeEach
  public void setup() throws IOException {
    Mockito.when(userPrincipalAuditorMapping.getCurrentAuditor())
        .thenReturn(Optional.of("test-user"));

    bagFile = TestBag.loadFile();
    projectRepository.save(ProjectBuilder.builder()
        .projectAbbr(TestProject.PROJECT_ABBR.getValue())
        .build());

    // ingest the bag
    byte[] zippedBag = ZipUtils.zipDir(bagFile);
    ingestService.ingest(
        TestProject.PROJECT_ABBR.getValue(),
        new ByteArrayInputStream(zippedBag)
    );

    // Index object
    apiSearchService.indexObject(
        TestProject.PROJECT_ABBR.getValue(),
        TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
    );
  }

  @Nested
  public class GET {

    String fulltextResponse;

    @BeforeEach
    public void setup() throws Exception {
      final String FULLTEXT_SEARCH_URL = String.format("%s?projects=%s&q=%s",FulltextController.FULLTEXT_SEARCH_PATH, TestProject.PROJECT_ABBR.getValue(), TestDublinCoreEntry.VALUE.getValue());

      fulltextResponse = mockMvc.perform(
              MockMvcRequestBuilders.get(FULLTEXT_SEARCH_URL)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();
    }

    @Test
    public void responseContainsExpectedHighlightedString() {

      Assertions.assertThat(fulltextResponse)
          .isNotNull()
          .isNotEmpty();

      final String EXPECTED_HIGHLIGHTING_STRING = FulltextSolrConfig.HIGHLIGHT_PRE.name + TestDublinCoreEntry.VALUE.getValue() + FulltextSolrConfig.HIGHLIGHT_POST.name;
      Assertions.assertThat(fulltextResponse)
          .contains(EXPECTED_HIGHLIGHTING_STRING);

    }

    @Test
    public void responseContainsExpectedDigitalObjectId() {
      Assertions.assertThat(fulltextResponse)
          .isNotNull()
          .isNotEmpty();

      final String EXPECTED_DIGITAL_OBJECT_ID_STRING = TestDigitalObject.DIGITAL_OBJECT_ID.getValue();
      Assertions.assertThat(fulltextResponse)
          .contains(EXPECTED_DIGITAL_OBJECT_ID_STRING);

    }

    @Test
    public void responseContainsExpectedObjectTags(){
      Assertions.assertThat(fulltextResponse)
          .isNotNull()
          .isNotEmpty();

      TestBag.TestBagSipJson.DIGITAL_OBJECT_TAGS.forEach(tag ->
          Assertions.assertThat(fulltextResponse)
              .contains(tag)
      );

    }

  }

  @Nested
  public class DCWordSearch {

    String fulltextDcSearchResponse;

    @BeforeEach
    public void setup() throws Exception {
      final String FULLTEXT_DC_SEARCH_URL = String.format(
          "%s?dc.rights=Commons",
          FulltextController.FULLTEXT_SEARCH_PATH

      );

      fulltextDcSearchResponse = mockMvc.perform(
              MockMvcRequestBuilders.get(FULLTEXT_DC_SEARCH_URL)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();
    }

    @Test
    public void dcSearchResponseContainsExpectedDigitalObjectId() {
      Assertions.assertThat(fulltextDcSearchResponse)
          .isNotNull()
          .isNotEmpty();

      final String EXPECTED_DIGITAL_OBJECT_ID_STRING = TestDigitalObject.DIGITAL_OBJECT_ID.getValue();
      Assertions.assertThat(fulltextDcSearchResponse)
          .contains(EXPECTED_DIGITAL_OBJECT_ID_STRING);

    }

  }

  @Nested
  public class DCPhraseSearch {

    String fulltextDcPhraseSearchResponse;

    @BeforeEach
    public void setup() throws Exception {
      final String FULLTEXT_DC_PHRASE_SEARCH_URL = String.format(
          "%s?dc.titleAsPhrase=%s",
          FulltextController.FULLTEXT_SEARCH_PATH,
          TestDublinCoreEntry.VALUE.getValue()
      );

      fulltextDcPhraseSearchResponse = mockMvc.perform(
              MockMvcRequestBuilders.get(FULLTEXT_DC_PHRASE_SEARCH_URL)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();
    }

    @Test
    public void dcPhraseSearchResponseContainsExpectedDigitalObjectId() {
      Assertions.assertThat(fulltextDcPhraseSearchResponse)
          .isNotNull()
          .isNotEmpty();

      final String EXPECTED_DIGITAL_OBJECT_ID_STRING = TestDigitalObject.DIGITAL_OBJECT_ID.getValue();
      Assertions.assertThat(fulltextDcPhraseSearchResponse)
          .contains(EXPECTED_DIGITAL_OBJECT_ID_STRING);

    }

  }

  @Nested
  public class ComplexSearch {

    @Test
    public void containsExpectedValues() throws Exception {
      final String FULLTEXT_DC_PHRASE_SEARCH_URL = String.format(
          "%s?q=%s&dc.titleAsPhrase=%s&dc.rights=Commons&dc.rights=Creative&dc.descriptionAsPhrase=test-dc-description",
          FulltextController.FULLTEXT_SEARCH_PATH,
          TestDublinCoreEntry.VALUE.getValue(),
          TestDublinCoreEntry.VALUE.getValue()
      );

      String response = mockMvc.perform(
              MockMvcRequestBuilders.get(FULLTEXT_DC_PHRASE_SEARCH_URL)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();

      Assertions.assertThat(response)
          .isNotNull()
          .isNotEmpty();

      final String EXPECTED_DIGITAL_OBJECT_ID_STRING = TestDigitalObject.DIGITAL_OBJECT_ID.getValue();
      Assertions.assertThat(response)
          .contains(EXPECTED_DIGITAL_OBJECT_ID_STRING)
      ;

      Assertions.assertThat(response)
          .contains(FulltextSolrConfig.HIGHLIGHT_PRE.name + TestDublinCoreEntry.VALUE.getValue() + FulltextSolrConfig.HIGHLIGHT_POST.name)
          .contains(
              TestDigitalObject.DIGITAL_OBJECT_PROJECT_ABBR.getValue(),
              TestDigitalObject.DIGITAL_OBJECT_TITLE.getValue(),
              TestDigitalObject.DIGITAL_OBJECT_CREATOR.getValue()
          );

    }

  }

  @Nested
  public class WebClient {

    String fulltextWebclientResponse = "";

    @BeforeEach
    public void setup() throws Exception {

      final String FULLTEXT_SEARCH_URL = String.format("%s?",FulltextController.FULLTEXT_SEARCH_PATH);

      fulltextWebclientResponse = mockMvc.perform(
              MockMvcRequestBuilders.get(FULLTEXT_SEARCH_URL)
                  .accept(MediaType.TEXT_HTML_VALUE)
          )
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();

    }

    @Test
    public void webclientResponseContainsExpectedData(){

      Assertions.assertThat(fulltextWebclientResponse)
          .contains(
              "Fulltext",
              TestDigitalObject.DIGITAL_OBJECT_ID.getValue(),
              TestDigitalObject.DIGITAL_OBJECT_TITLE.getValue(),
              TestDigitalObject.DIGITAL_OBJECT_PROJECT_ABBR.getValue()
          );

    }

  }

}
