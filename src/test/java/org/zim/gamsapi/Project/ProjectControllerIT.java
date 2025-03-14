package org.zim.gamsapi.Project;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestProject;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc(addFilters = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProjectControllerIT extends IntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private IProjectRepository projectRepository;

  // disables auditing
  // (necessary -> otherwise the createdBy fields etc. from Project need to be filled)
  // this auditing / security test is done in a separate test
  @MockBean
  private AuditingHandler auditingHandler;


  @Nested
  public class WebclientTest {

    @Test
    public void projectAbbrContainedInWebclientProjectsOverview() throws Exception {

      Project project = ProjectBuilder.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build();

      projectRepository.save(project);

      MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/projects/").accept(MediaType.TEXT_HTML))
        .andExpect(MockMvcResultMatchers.status().isOk())
          // verifies that webclient html is being returned
          .andExpect(MockMvcResultMatchers.content().contentType("text/html;charset=UTF-8"))
          .andReturn();

      Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(project.getProjectAbbr());

    }

  }

  @Nested
  public class ProjectCreation {

    /**
     * Tests if a PUT request for creating a project returns https status 200.
     * Deactivated security filters and auditing for this test (done at class level).
     * @throws Exception if the test fails (mockMvc.perform)
     */
    @Test
    public void PUTTestProjectReturns200() throws Exception {

      final String TEST_PROJECT_URL = String.format(
          "/api/v1/projects/%s", TestProject.PROJECT_ABBR.getValue()
      );

      // first create a project
      mockMvc.perform(
          MockMvcRequestBuilders.put(TEST_PROJECT_URL)
      ).andExpect(status().isOk());

    }

  }

  @Nested
  public class ProjectDeletion {

    @Test
    public void DELETEofProjectReturnsHTTPStatus200() throws Exception {

      projectRepository.save(TestProject.generate());

      mockMvc.perform(
          MockMvcRequestBuilders.delete(
              String.format("/api/v1/projects/%s", TestProject.PROJECT_ABBR.getValue())
          )
      ).andExpect(status().isOk());

    }

  }



}
