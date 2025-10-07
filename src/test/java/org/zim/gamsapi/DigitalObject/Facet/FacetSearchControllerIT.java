package org.zim.gamsapi.DigitalObject.Facet;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.TestUtilities.TestDataBuilder;
import org.zim.gamsapi.TestUtilities.TestDataSet;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false) // deactivates spring security for the test class
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FacetSearchControllerIT extends IntegrationTest {


  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private MockMvc mockMvc;

  // disable auditing for this test class
  @MockitoBean
  private AuditingHandler auditingHandler;

  private TestDataSet testDataSet;

  @Autowired
  private TestDataBuilder testDataBuilder;

  final String FACETS_SEARCH_ENDPOINT = "/api/v1/facets";

  @BeforeEach
  public void setUp() {
    testDataSet = testDataBuilder.buildTestDataSet();
  }


  @Test
  public void containsExpectedResult() throws Exception {

    final String REQUEST_URL = String.format(
        "%s?projects=%s",
        FACETS_SEARCH_ENDPOINT,
        testDataSet.project().getProjectAbbr()
    );

    MvcResult mvcResult = mockMvc.perform(
            MockMvcRequestBuilders.get(REQUEST_URL)
        )
        .andExpect(status().isOk())
        .andExpect(result -> result
            .getResponse()
            .getContentType().equals(MediaType.APPLICATION_JSON_VALUE))
        .andReturn();

    Assertions.assertThat(mvcResult.getResponse().getContentAsString())
        .contains(testDataSet.project().getProjectAbbr())
        .contains(testDataSet.digitalObject().getId())
        .contains(testDataSet.digitalObject().getBaseMetadata().getTitle())
        .contains(testDataSet.dublinCoreEntry().getName())
        .contains(testDataSet.dublinCoreEntry().getValue());

  }

  @Test
  public void containsExpectedMainResourceMetadata() throws Exception {
    final String REQUEST_URL = String.format(
        "%s?projects=%s",
        FACETS_SEARCH_ENDPOINT,
        testDataSet.project().getProjectAbbr()
    );

    MvcResult mvcResult = mockMvc.perform(
            MockMvcRequestBuilders.get(REQUEST_URL)
        )
        .andExpect(status().isOk())
        .andExpect(result -> result
            .getResponse()
            .getContentType().equals(MediaType.APPLICATION_JSON_VALUE))
        .andReturn();

    Assertions.assertThat(mvcResult.getResponse().getContentAsString())
        .contains(testDataSet.project().getProjectAbbr())
        .contains(testDataSet.digitalObject().getId())
        .contains(testDataSet.mainDatastream().getTags())
        .contains(testDataSet.mainDatastream().getLang())
        .contains(testDataSet.mainDatastream().getDsid())
        .contains(testDataSet.mainDatastream().getMimeType())
        .contains(testDataSet.mainDatastream().getBaseMetadata().getTitle())
        .contains(testDataSet.mainDatastream().getBaseMetadata().getDescription());
  }

}
