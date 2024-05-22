package org.zim.gamsapi.System.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockPart;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.zim.gamsapi.Ingest.utils.IngestStatics;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.System.security.exceptions.UserNotAssignedToProjectException;
import org.zim.gamsapi.enums.TestProject;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests Authorization settings in the application
 */
@AutoConfigureMockMvc
public class AuthorizationIT extends IntegrationTest {


  @Autowired
  private MockMvc mockMvc;


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
  public void projectAdminAuthorizedForProjectIngest_throwsServerErrorAtProcessingAfterBeingAuthorized() throws Exception {

    byte[] zippedBag = new byte[0];
    MockPart mockPart = new MockPart(IngestStatics.FORM_PART_NAME.name, "test.zip", zippedBag);

    String testProjectAdminRole = GAMSAPISecurityRoles.getProjectAdmin(TestProject.PROJECT_ABBR.getValue());
    // mock method needs role prefix excluded.
    testProjectAdminRole = testProjectAdminRole.replace(GAMSAPISecurityRoles.ROLE_PREFIX.name, "");

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
            status().is5xxServerError()
        );

  }

  @Test
  public void globalAdminMayIngest_throwsServerErrorAtProcessingAfterBeingAuthorized() throws Exception {

    byte[] zippedBag = new byte[0];
    MockPart mockPart = new MockPart(IngestStatics.FORM_PART_NAME.name, "test.zip", zippedBag);

    String globalAdminRole = GAMSAPISecurityRoles.getAdmin();
    // mock method needs role prefix excluded.
    globalAdminRole = globalAdminRole.replace(GAMSAPISecurityRoles.ROLE_PREFIX.name, "");

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
            status().is5xxServerError()
        );
  }

  @Test
  public void projectAdminAuthorizedForDifferentProjectIngest_throwsUserNotAssignedToProjectException() {

    byte[] zippedBag = new byte[0];
    MockPart mockPart = new MockPart(IngestStatics.FORM_PART_NAME.name, "test.zip", zippedBag);

    // mock method needs role prefix excluded.
    String differentProjectAdminRole = GAMSAPISecurityRoles.getProjectAdmin("differentproject")
        .replace(GAMSAPISecurityRoles.ROLE_PREFIX.name, "");

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



}
