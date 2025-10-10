package org.zim.gamsapi.System.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockPart;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.zim.gamsapi.DigitalObject.Ingest.utils.IngestStatics;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.System.security.exceptions.UserNotAssignedToProjectException;
import org.zim.gamsapi.TestUtilities.TestProject;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests Authorization settings in the application
 */
@AutoConfigureMockMvc
public class AuthorizationIT extends IntegrationTest {


  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private IProjectRepository projectRepository;


  @Test
  public void authenticatedDemoUserNotAuthorizedForProjectIngest_throwsUserNotAssignedToProjectException() {

    byte[] zippedBag = new byte[0];
    MockPart mockPart = new MockPart(IngestStatics.FORM_PART_NAME.name, "test.zip", zippedBag);

    Assertions.assertThrows(UserNotAssignedToProjectException.class, () -> {
      mockMvc
          .perform(
              multipart("/api/v1/projects/{projectAbbr}/objects", TestProject.PROJECT_ABBR.getValue())
                  .part(mockPart)
                  .with(SecurityMockMvcRequestPostProcessors
                      .user("UNKNOWN_USER")
                      .roles("UNKNOWN_ROLE")
                  )
          )
          .andExpect(status().is4xxClientError());
    });

  }

  @Test
  public void projectAdminAuthorizedForProjectIngest_throwsExpected404ErrorBecauseProjectDoesNotExist() throws Exception {

    byte[] zippedBag = new byte[0];
    MockPart mockPart = new MockPart(IngestStatics.FORM_PART_NAME.name, "test.zip", zippedBag);

    String testProjectAdminRole = GAMSAPIAuthorities.convertToRole(
        GAMSAPIAuthorities.getProjectAdmin(TestProject.PROJECT_ABBR.getValue())
    );

    mockMvc
        .perform(
            multipart("/api/v1/projects/{projectAbbr}/objects", TestProject.PROJECT_ABBR.getValue())
                .part(mockPart)
                .with(SecurityMockMvcRequestPostProcessors
                    .user("SOME_USER")
                    .roles(testProjectAdminRole)
                )
        )
        .andExpect(
            status().isNotFound()
        );

  }

  @Test
  public void globalAdminMayIngest_throwsExpected404ErrorBecauseProjectDoesNotExist() throws Exception {

    byte[] zippedBag = new byte[0];
    MockPart mockPart = new MockPart(IngestStatics.FORM_PART_NAME.name, "test.zip", zippedBag);

    String globalAdminRole = GAMSAPIAuthorities.convertToRole(GAMSAPIAuthorities.getAdmin());

    mockMvc
        .perform(
            multipart("/api/v1/projects/{projectAbbr}/objects", TestProject.PROJECT_ABBR.getValue())
                .part(mockPart)
                .with(SecurityMockMvcRequestPostProcessors
                    .user("SOME_USER")
                    .roles(globalAdminRole)
                )
        )
        .andExpect(
            status().isNotFound()
        );
  }

  @Test
  public void projectAdminAuthorizedForDifferentProjectIngest_throwsUserNotAssignedToProjectException() {

    byte[] zippedBag = new byte[0];
    MockPart mockPart = new MockPart(IngestStatics.FORM_PART_NAME.name, "test.zip", zippedBag);

    // mock method needs role prefix excluded.
    String differentProjectAdminRole = GAMSAPIAuthorities.convertToRole(GAMSAPIAuthorities.getProjectAdmin("differentproject"));

    Assertions.assertThrows(UserNotAssignedToProjectException.class, () -> {
      mockMvc
          .perform(
              multipart("/api/v1/projects/{projectAbbr}/objects", TestProject.PROJECT_ABBR.getValue())
                  .part(mockPart)
                  .with(SecurityMockMvcRequestPostProcessors
                      .user("SOME_USER")
                      .roles(differentProjectAdminRole)
                  )
          ).andExpect(status().is4xxClientError());
    });

  }

  @Nested
  public class ProjectAuthorization {

    @Test
    public void anonymousUserNotAuthorizedForProjectCreation_redirects() throws Exception {

      final String TEST_PROJECT_ABBR = "FOO";
      final String TEST_URL = "/api/v1/projects/" + TEST_PROJECT_ABBR;

      mockMvc
          .perform(
              MockMvcRequestBuilders.post(TEST_URL)
                  .with(SecurityMockMvcRequestPostProcessors.anonymous())
          )
          .andExpect(status().is3xxRedirection());
    }

    @Test
    public void adminMayCreateAProject() throws Exception {

      final String TEST_PROJECT_ABBR = TestProject.PROJECT_ABBR.getValue();
      final String TEST_URL = "/api/v1/projects/" + TEST_PROJECT_ABBR;

      mockMvc
          .perform(
              MockMvcRequestBuilders.put(TEST_URL)
                  .with(
                      SecurityMockMvcRequestPostProcessors.oidcLogin().authorities(new SimpleGrantedAuthority(GAMSAPIAuthorities.getAdmin()))
                  )
          ).andExpect(status().is2xxSuccessful());

      org.assertj.core.api.Assertions.assertThat(projectRepository.findById(TEST_PROJECT_ABBR))
          .isPresent();

      // cleanup
      projectRepository.deleteAll();

    }

    @Test
    public void anonymousUserNotAuthorizedForProjectDeletion_redirects() throws Exception {

      final String TEST_PROJECT_ABBR = "FOO";
      final String TEST_URL = "/api/v1/projects/" + TEST_PROJECT_ABBR;

      mockMvc
          .perform(
              MockMvcRequestBuilders.delete(TEST_URL)
                  .with(SecurityMockMvcRequestPostProcessors.anonymous())
          )
          .andExpect(status().is3xxRedirection());
    }


  }

}
