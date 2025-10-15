package org.zim.gamsapi.infrastructure.System;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.MimeTypeUtils;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.TestUtilities.TestDataBuilder;
import org.zim.gamsapi.TestUtilities.TestDataSet;
import org.zim.gamsapi.TestUtilities.TestDublinCoreEntry;

import java.util.Set;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
public class SearchControllerIT extends IntegrationTest {

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private MockMvc mockMvc;

  // disables auditing
  // (necessary -> otherwise the createdBy fields etc. from Project need to be filled)
  // this auditing / security test is done in a separate test
  @MockitoBean
  private AuditingHandler auditingHandler;

  private TestDataSet testDataSet;

  @Autowired
  private TestDataBuilder testDataBuilder;

  @Nested
  public class DublinCoreSearch {


    @BeforeEach
    public void setup() {
      testDataSet = testDataBuilder.buildTestDataSet();
    }

    @Nested
    public class DCFulltextSearch {

      final String FULLTEXT_SEARCH_URL_TEMPLATE = "/api/v1/search/dc/fulltext?projects=%s&search=%s";

      @Test
      public void findsExpectedTestObjectJSON_whenSearchingOverAllDCFields() throws Exception {

        // arbitrary substring of test data
        final String TEST_FULLTEXT_QUERY = TestDublinCoreEntry.VALUE.getValue().substring(0,3);

        String requestUrl = String.format(
            FULLTEXT_SEARCH_URL_TEMPLATE,
            testDataSet.project().getProjectAbbr(),
            TEST_FULLTEXT_QUERY
        );
        String response = mockMvc.perform(
                MockMvcRequestBuilders.get(requestUrl)
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();


        Assertions.assertThat(response)
            .contains(
                testDataSet.digitalObject().getId(),
                testDataSet.digitalObject().getProject().getProjectAbbr(),
                testDataSet.digitalObject().getBaseMetadata().getTitle(),
                testDataSet.digitalObject().getBaseMetadata().getDescription()
            );

      }

      @Test
      public void findsNothingWhenRestrictedToANonOccurringDCField() throws Exception {

        // arbitrary substring of test data
        final String TEST_FULLTEXT_QUERY = TestDublinCoreEntry.VALUE.getValue().substring(0,3);
        final Set<String> TEST_NON_OCCURRING_DC_FIELDS = Set.of("type", "format");

        String REQUEST_URL = String.format(
            "/api/v1/search/dc/fulltext?projects=%s&search=%s",
            testDataSet.project().getProjectAbbr(),
            TEST_FULLTEXT_QUERY
        );

        // add dcFields to request url
        REQUEST_URL += TEST_NON_OCCURRING_DC_FIELDS.stream()
            .reduce("", (acc, field) -> acc + "&dcFields=" + field);

        String response = mockMvc.perform(
                MockMvcRequestBuilders.get(REQUEST_URL)
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();


        Assertions.assertThat(response)
            .doesNotContain(
                testDataSet.digitalObject().getId(),
                testDataSet.digitalObject().getProject().getProjectAbbr(),
                testDataSet.digitalObject().getBaseMetadata().getTitle(),
                testDataSet.digitalObject().getBaseMetadata().getDescription()
            );

      }

    }

    @Nested
    public class DublinCoreFieldSearch {

      final String DC_SEARCH_BASE_URL = "/api/v1/search/dc";
      String TEST_DC_URL_QUERY;
      String TEST_DC_SEARCH_REQUEST_URL;

      @BeforeEach
      public void setup() {
        TEST_DC_URL_QUERY = "dc." + testDataSet.dublinCoreEntry().getName() + "=" + testDataSet.dublinCoreEntry().getValue();
        TEST_DC_SEARCH_REQUEST_URL = String.format("%s?projects=%s&%s",
            DC_SEARCH_BASE_URL,
            testDataSet.project().getProjectAbbr(),
            TEST_DC_URL_QUERY
        );
      }

      @Nested
      public class ResponseBodyTests {

        @Test
        public void returnedSearchJsonContainsExpectedDCValues() throws Exception {
          String response = mockMvc.perform(
                  MockMvcRequestBuilders.get(TEST_DC_SEARCH_REQUEST_URL)
                      .accept(MediaType.APPLICATION_JSON)
              )
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

          Assertions.assertThat(response)
              .contains(testDataSet.dublinCoreEntry().getName(), testDataSet.dublinCoreEntry().getValue());
        }

        @Test
        public void returnedSearchJsonContainsExpectedMainResourceMetadata() throws Exception {
          String response = mockMvc.perform(
                  MockMvcRequestBuilders.get(TEST_DC_SEARCH_REQUEST_URL)
                      .accept(MediaType.APPLICATION_JSON)
              )
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

          Assertions.assertThat(response)
              .contains(
                  testDataSet.mainDatastream().getMimeType(),
                  testDataSet.mainDatastream().getDsid(),
                  testDataSet.mainDatastream().getBaseMetadata().getDescription(),
                  testDataSet.mainDatastream().getBaseMetadata().getTitle()
              );

          Assertions.assertThat(response).contains(testDataSet.mainDatastream().getTags());
          Assertions.assertThat(response).contains(testDataSet.mainDatastream().getLang());

        }

        @Test
        public void projectsUrlParamMightNotBeEmpty() throws Exception {

          final String TEST_DC_SEARCH_REQUEST_URL_EMPTY_PROJECTS = String.format(
              "%s?%s",
              DC_SEARCH_BASE_URL,
              TEST_DC_URL_QUERY
          );

          mockMvc.perform(
                  MockMvcRequestBuilders.get(TEST_DC_SEARCH_REQUEST_URL_EMPTY_PROJECTS)
                      .accept(MediaType.APPLICATION_JSON)
              )
              .andExpect(status().is4xxClientError());
        }

        @Test
        public void acceptXmlWillReturnExpectedDcSearchValues() throws Exception {
          String response = mockMvc.perform(
                  MockMvcRequestBuilders.get(TEST_DC_SEARCH_REQUEST_URL)
                      .accept(MimeTypeUtils.APPLICATION_XML_VALUE)
              )
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

          Assertions.assertThat(response)
              .contains(testDataSet.dublinCoreEntry().getName(), testDataSet.dublinCoreEntry().getValue());
        }

        @Test
        public void formatXmlWillReturnExpectedDcSearchValues() throws Exception {
          // append format=xml to request url
          final String TEST_DC_SEARCH_REQUEST_URL_XML_FORMAT = String.format(
              "%s&format=xml",
              TEST_DC_SEARCH_REQUEST_URL
          );

          String response = mockMvc.perform(
                  MockMvcRequestBuilders.get(TEST_DC_SEARCH_REQUEST_URL_XML_FORMAT)
              )
              .andExpect(status().isOk())
              .andExpect(
                  result -> result.getResponse().getContentType()
                      .equals(MimeTypeUtils.APPLICATION_XML_VALUE)
              )
              .andReturn()
              .getResponse()
              .getContentAsString();

          Assertions.assertThat(response)
              .contains(testDataSet.dublinCoreEntry().getName(), testDataSet.dublinCoreEntry().getValue(), "<", ">");
        }

      }

      @Nested
      public class Webclient {

        @Test
        public void webclientContainsExpectedDcSearchValues() throws Exception {
          String response = mockMvc.perform(
                  MockMvcRequestBuilders.get(TEST_DC_SEARCH_REQUEST_URL)
                      .accept(MimeTypeUtils.TEXT_HTML_VALUE)
              )
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

          Assertions.assertThat(response)
              .contains(testDataSet.dublinCoreEntry().getName(), testDataSet.dublinCoreEntry().getValue());

        }

      }


    }

  }

}
