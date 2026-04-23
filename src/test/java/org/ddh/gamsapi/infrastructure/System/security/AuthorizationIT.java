package org.ddh.gamsapi.infrastructure.System.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.ddh.gamsapi.application.Ingest.utils.IngestStatics;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.ddh.gamsapi.infrastructure.System.security.exceptions.UserNotAssignedToProjectException;
import org.ddh.gamsapi.TestUtilities.TestProject;
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

  // disables auditing
  // (necessary -> otherwise the createdBy fields etc. from Project need to be filled)
  // this auditing / security test is done in a separate test
  @MockitoBean
  private AuditingHandler auditingHandler;


  @Test
  void authenticatedDemoUserNotAuthorizedForProjectIngest_returnsStatus403() throws Exception {

    byte[] zippedBag = new byte[0];
    MockPart mockPart = new MockPart(IngestStatics.FORM_PART_NAME.name, "test.zip", zippedBag);


    mockMvc
        .perform(
            multipart("/api/curation/v1/projects/{projectAbbr}/objects", TestProject.PROJECT_ABBR.getValue())
                .part(mockPart)
                .with(SecurityMockMvcRequestPostProcessors
                    .user("UNKNOWN_USER")
                    .roles("UNKNOWN_ROLE")
                )
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        )
        .andExpect(status().is(403));


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
            multipart("/api/curation/v1/projects/{projectAbbr}/objects", TestProject.PROJECT_ABBR.getValue())
                .part(mockPart)
                .with(SecurityMockMvcRequestPostProcessors
                    .user("SOME_USER")
                    .roles(testProjectAdminRole)
                )
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        )
        .andExpect(
            status().isNotFound()
        );

  }

  @Test
  public void globalAdminMayIngest_throwsExpected404ErrorBecauseProjectDoesNotExist() throws Exception {

    byte[] zippedBag = new byte[0];
    MockPart mockPart = new MockPart(IngestStatics.FORM_PART_NAME.name, "test.zip", zippedBag);

    String globalAdminRole = GAMSAPIAuthorities.convertToRole(GAMSAPIAuthorities.getSuperAdmin());

    mockMvc
        .perform(
            multipart("/api/curation/v1/projects/{projectAbbr}/objects", TestProject.PROJECT_ABBR.getValue())
                .part(mockPart)
                .with(SecurityMockMvcRequestPostProcessors
                    .user("SOME_USER")
                    .roles(globalAdminRole)
                )
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        )
        .andExpect(
            status().isNotFound()
        );
  }

  @Test
  void projectAdminAuthorizedForDifferentProjectIngest_returnsStatus403() throws Exception {

    byte[] zippedBag = new byte[0];
    MockPart mockPart = new MockPart(IngestStatics.FORM_PART_NAME.name, "test.zip", zippedBag);

    // mock method needs role prefix excluded.
    String differentProjectAdminRole = GAMSAPIAuthorities.convertToRole(GAMSAPIAuthorities.getProjectAdmin("differentproject"));


    mockMvc
        .perform(
            multipart("/api/curation/v1/projects/{projectAbbr}/objects", TestProject.PROJECT_ABBR.getValue())
                .part(mockPart)
                .with(SecurityMockMvcRequestPostProcessors
                    .user("SOME_USER")
                    .roles(differentProjectAdminRole)
                )
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().is(403));


  }

  @Nested
  public class ProjectAuthorization {

    @Test
    public void anonymousUserNotAuthorizedForProjectCreation_redirects() throws Exception {

      final String TEST_PROJECT_ABBR = "FOO";
      final String TEST_URL = "/api/curation/v1/projects/" + TEST_PROJECT_ABBR;

      mockMvc
          .perform(
              MockMvcRequestBuilders.post(TEST_URL)
                  .with(SecurityMockMvcRequestPostProcessors.anonymous())
                  .with(SecurityMockMvcRequestPostProcessors.csrf())
          )
          .andExpect(status().is3xxRedirection());
    }

    @Test
    public void adminMayCreateAProject() throws Exception {

      final String TEST_PROJECT_ABBR = TestProject.PROJECT_ABBR.getValue();
      final String TEST_URL = "/api/curation/v1/projects/" + TEST_PROJECT_ABBR;

      mockMvc
          .perform(
              MockMvcRequestBuilders.put(TEST_URL)
                  .with(
                      SecurityMockMvcRequestPostProcessors.oidcLogin().authorities(new SimpleGrantedAuthority(GAMSAPIAuthorities.getSuperAdmin()))
                  )
                  .with(SecurityMockMvcRequestPostProcessors.csrf())
          ).andExpect(status().is2xxSuccessful());

      org.assertj.core.api.Assertions.assertThat(projectRepository.findById(TEST_PROJECT_ABBR))
          .isPresent();

      // cleanup
      projectRepository.deleteAll();

    }

    @Test
    public void anonymousUserNotAuthorizedForProjectDeletion_redirects() throws Exception {

      final String TEST_PROJECT_ABBR = "FOO";
      final String TEST_URL = "/api/curation/v1/projects/" + TEST_PROJECT_ABBR;

      mockMvc
          .perform(
              MockMvcRequestBuilders.delete(TEST_URL)
                  .with(SecurityMockMvcRequestPostProcessors.anonymous())
                  .with(SecurityMockMvcRequestPostProcessors.csrf())
          )
          .andExpect(status().is3xxRedirection());
    }

    @Test
    void userNotAssignedToProjectRoles_putProjectWillReturnStatusCode403() throws Exception {

      final String TEST_PROJECT_ABBR = TestProject.PROJECT_ABBR.getValue();
      final String TEST_URL = "/api/curation/v1/projects/" + TEST_PROJECT_ABBR;

      mockMvc
          .perform(
              MockMvcRequestBuilders.put(TEST_URL)
                  .with(
                      // user is authenticated but has no roles
                      SecurityMockMvcRequestPostProcessors.oidcLogin()
                  )
                  .with(SecurityMockMvcRequestPostProcessors.csrf())
          ).andExpect(status().is(403));

    }

    @Test
    void userAssignedToDifferentProject_putProjectWillReturnStatusCode403() throws Exception {

      final String TEST_PROJECT_ABBR = TestProject.PROJECT_ABBR.getValue();
      final String TEST_URL = "/api/curation/v1/projects/" + TEST_PROJECT_ABBR;

      mockMvc
          .perform(
              MockMvcRequestBuilders.put(TEST_URL)
                  .with(
                      // user is assigned to a different project
                      SecurityMockMvcRequestPostProcessors.oidcLogin().authorities(
                          new SimpleGrantedAuthority(GAMSAPIAuthorities.getProjectAdmin("different"))
                      )
                  )
                  .with(SecurityMockMvcRequestPostProcessors.csrf())
          ).andExpect(status().is(403));

    }


    @Test
    void userAssignedToDifferentProject_patchAnotherProjectWillReturnStatusCode403() throws Exception {

      // create test project (so that it can be patched)
      projectRepository.save(TestProject.generate());

      final String TEST_PROJECT_ABBR = TestProject.PROJECT_ABBR.getValue();
      final String TEST_URL = "/api/curation/v1/projects/" + TEST_PROJECT_ABBR;

      // update the project description
      final String UPDATED_TEST_PROJECT_DESCRIPTION = "Updated description";
      final String TEST_PROJECT_PATCH_REQUEST_BODY =  "{\"description\": \"" + UPDATED_TEST_PROJECT_DESCRIPTION + "\"}";

      mockMvc
          .perform(
              MockMvcRequestBuilders.patch(TEST_URL)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(TEST_PROJECT_PATCH_REQUEST_BODY)
                  .with(
                      // user is assigned to a different project
                      SecurityMockMvcRequestPostProcessors.oidcLogin().authorities(
                          new SimpleGrantedAuthority(GAMSAPIAuthorities.getProjectAdmin("different"))
                      )
                  )
                  .with(SecurityMockMvcRequestPostProcessors.csrf())
          ).andExpect(status().is(403));

    }

    @Test
    void userAssignedToDifferentProject_deleteAnotherProjectWillReturnStatusCode403() throws Exception {

      // create test project (so that it can be patched)
      projectRepository.save(TestProject.generate());

      final String TEST_PROJECT_ABBR = TestProject.PROJECT_ABBR.getValue();
      final String TEST_URL = "/api/curation/v1/projects/" + TEST_PROJECT_ABBR;

      mockMvc
          .perform(
              MockMvcRequestBuilders.delete(TEST_URL)
                  .with(
                      // user is assigned to a different project
                      SecurityMockMvcRequestPostProcessors.oidcLogin().authorities(
                          new SimpleGrantedAuthority(GAMSAPIAuthorities.getProjectAdmin("different"))
                      )
                  )
                  .with(SecurityMockMvcRequestPostProcessors.csrf())
          ).andExpect(status().is(403));

    }


  }

}
