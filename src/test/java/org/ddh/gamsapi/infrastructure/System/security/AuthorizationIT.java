package org.ddh.gamsapi.infrastructure.System.security;

import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.application.Ingest.utils.IngestStatics;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.hamcrest.Matchers;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests Authorization settings in the application
 */
@AutoConfigureMockMvc
class AuthorizationIT extends IntegrationTest {


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
  void projectAdminAuthorizedForProjectIngest_throwsExpected404ErrorBecauseProjectDoesNotExist() throws Exception {

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
  void globalAdminMayIngest_throwsExpected404ErrorBecauseProjectDoesNotExist() throws Exception {

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
  class ProjectAuthorization {

    @Test
    void anonymousUserNotAuthorizedForProjectCreation_redirects() throws Exception {

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
    void adminMayCreateAProject() throws Exception {

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
    void anonymousUserNotAuthorizedForProjectDeletion_redirects() throws Exception {

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

  @Nested
  class ArchivalRecordAuthorization {

    private final String TEST_REQUEST_BODY = "{}";

    private final String ARCHIVAL_RECORDS_URL = String.format(
        "/api/curation/v1/projects/%s/objects/%s/archival-records",
        TestProject.PROJECT_ABBR.getValue(), TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
    );

    @Test
    void projectAdminIsForbiddenToPost() throws Exception {
      mockMvc.perform(
          MockMvcRequestBuilders.post(ARCHIVAL_RECORDS_URL)
              .contentType(MediaType.APPLICATION_JSON)
              .content(TEST_REQUEST_BODY)
              .with(SecurityMockMvcRequestPostProcessors.oidcLogin().authorities(
                  new SimpleGrantedAuthority(GAMSAPIAuthorities.getProjectAdmin(TestProject.PROJECT_ABBR.getValue()))
              ))
              .with(SecurityMockMvcRequestPostProcessors.csrf())
      ).andExpect(status().is(403));
    }

    @Test
    void projectEditorIsForbiddenToPost() throws Exception {
      mockMvc.perform(
          MockMvcRequestBuilders.post(ARCHIVAL_RECORDS_URL)
              .contentType(MediaType.APPLICATION_JSON)
              .content(TEST_REQUEST_BODY)
              .with(SecurityMockMvcRequestPostProcessors.oidcLogin().authorities(
                  new SimpleGrantedAuthority(GAMSAPIAuthorities.getProjectEditor(TestProject.PROJECT_ABBR.getValue()))
              ))
              .with(SecurityMockMvcRequestPostProcessors.csrf())
      ).andExpect(status().is(403));
    }

    @Test
    void projectAdminIsForbiddenToDeleteNestedRecord() throws Exception {
      mockMvc.perform(
          MockMvcRequestBuilders.delete(ARCHIVAL_RECORDS_URL + "/1")
              .with(SecurityMockMvcRequestPostProcessors.oidcLogin().authorities(
                  new SimpleGrantedAuthority(GAMSAPIAuthorities.getProjectAdmin(TestProject.PROJECT_ABBR.getValue()))
              ))
              .with(SecurityMockMvcRequestPostProcessors.csrf())
      ).andExpect(status().is(403));
    }

    @Test
    void superAdminIsNotForbiddenToPost() throws Exception {
      // no digital object persisted in this test -> not asserting 2xx, only that
      // the authorization layer itself doesn't block it. Functional CRUD behavior
      // (given a real digital object) is covered in ArchivalRecordControllerIT.
      mockMvc.perform(
          MockMvcRequestBuilders.post(ARCHIVAL_RECORDS_URL)
              .contentType(MediaType.APPLICATION_JSON)
              .content(TEST_REQUEST_BODY)
              .with(SecurityMockMvcRequestPostProcessors.oidcLogin().authorities(
                  new SimpleGrantedAuthority(GAMSAPIAuthorities.getSuperAdmin())
              ))
              .with(SecurityMockMvcRequestPostProcessors.csrf())
      ).andExpect(status().is(Matchers.not(403)));
    }

    @Test
    void projectsAdminIsNotForbiddenToPost() throws Exception {
      mockMvc.perform(
          MockMvcRequestBuilders.post(ARCHIVAL_RECORDS_URL)
              .contentType(MediaType.APPLICATION_JSON)
              .content(TEST_REQUEST_BODY)
              .with(SecurityMockMvcRequestPostProcessors.oidcLogin().authorities(
                  new SimpleGrantedAuthority(GAMSAPIAuthorities.getProjectsAdministrator())
              ))
              .with(SecurityMockMvcRequestPostProcessors.csrf())
      ).andExpect(status().is(Matchers.not(403)));
    }

    @Test
    void anonymousUserIsRedirectedOnPost() throws Exception {
      mockMvc.perform(
          MockMvcRequestBuilders.post(ARCHIVAL_RECORDS_URL)
              .contentType(MediaType.APPLICATION_JSON)
              .content(TEST_REQUEST_BODY)
              .with(SecurityMockMvcRequestPostProcessors.anonymous())
              .with(SecurityMockMvcRequestPostProcessors.csrf())
      ).andExpect(status().is3xxRedirection());
    }

    @Test
    void projectAdminIsStillAllowedToRead() throws Exception {
      // sanity check: GET/HEAD/OPTIONS permitAll rule is registered earlier in the
      // chain, so it should never even reach the archival-records matcher
      mockMvc.perform(
          MockMvcRequestBuilders.get(ARCHIVAL_RECORDS_URL)
              .with(SecurityMockMvcRequestPostProcessors.oidcLogin().authorities(
                  new SimpleGrantedAuthority(GAMSAPIAuthorities.getProjectAdmin(TestProject.PROJECT_ABBR.getValue()))
              ))
      ).andExpect(status().is(Matchers.not(403)));
    }
  }

}
