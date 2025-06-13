package org.zim.gamsapi.System;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntry;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestDublinCoreEntry;
import org.zim.gamsapi.enums.TestProject;

import java.util.Set;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
public class SearchControllerIT extends IntegrationTest {

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private IProjectRepository projectRepository;

  @Autowired
  private IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  private IDublinCoreEntryRepository dublinCoreEntryRepository;

  // disables auditing
  // (necessary -> otherwise the createdBy fields etc. from Project need to be filled)
  // this auditing / security test is done in a separate test
  @MockitoBean
  private AuditingHandler auditingHandler;

  @Nested
  public class DublinCoreSearch {

    Project testProject = TestProject.generate();
    DigitalObject testDigitalObject = TestDigitalObject.generate();
    DublinCoreEntry testDublinCoreEntry = TestDublinCoreEntry.generate(testDigitalObject.getId());

    final String SEARCH_URL_TEMPLATE = "/api/v1/search/dc?projectAbbrs=%s&dcField=%s&search=%s";

    @BeforeEach
    public void setup() {
      projectRepository.save(testProject);
      digitalObjectRepository.save(testDigitalObject);
      dublinCoreEntryRepository.save(testDublinCoreEntry);
    }

    @Test
    public void GETDublinCoreEntryWithContainsReturnsExpectedTestObject() throws Exception {
      String requestUrl = String.format(
          SEARCH_URL_TEMPLATE,
          testProject.getProjectAbbr(),
          testDublinCoreEntry.getName(),
          testDublinCoreEntry.getValue()
      );

      String response = mockMvc.perform(
              MockMvcRequestBuilders.get(requestUrl)
          ).andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();

      Assertions.assertThat(response)
          .contains(
              testDigitalObject.getId(),
              testDigitalObject.getProject().getProjectAbbr(),
              testDigitalObject.getBaseMetadata().getTitle(),
              testDigitalObject.getBaseMetadata().getDescription()
          );

    }

    @Test
    public void returnsErrorIfSearchParamsWereNotDefined() throws Exception {
      final String MALFORMED_URL = "/api/v1/search/dc?projectAbbrs=%s";
      mockMvc.perform(
          MockMvcRequestBuilders.get(MALFORMED_URL)
      ).andExpect(
          status().isBadRequest()
      );


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
            testProject.getProjectAbbr(),
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
                testDigitalObject.getId(),
                testDigitalObject.getProject().getProjectAbbr(),
                testDigitalObject.getBaseMetadata().getTitle(),
                testDigitalObject.getBaseMetadata().getDescription()
            );

      }

      @Test
      public void findsNothingWhenRestrictedToANonOccurringDCField() throws Exception {

        // arbitrary substring of test data
        final String TEST_FULLTEXT_QUERY = TestDublinCoreEntry.VALUE.getValue().substring(0,3);
        final Set<String> TEST_NON_OCCURRING_DC_FIELDS = Set.of("type", "format");

        String REQUEST_URL = String.format(
            "/api/v1/search/dc/fulltext?projects=%s&search=%s",
            testProject.getProjectAbbr(),
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
                testDigitalObject.getId(),
                testDigitalObject.getProject().getProjectAbbr(),
                testDigitalObject.getBaseMetadata().getTitle(),
                testDigitalObject.getBaseMetadata().getDescription()
            );

      }

    }

  }

}
