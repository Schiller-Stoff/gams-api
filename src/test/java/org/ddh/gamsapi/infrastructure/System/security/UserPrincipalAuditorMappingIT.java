package org.ddh.gamsapi.infrastructure.System.security;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.application.Ingest.utils.IngestStatics;
import org.ddh.gamsapi.application.Ingest.utils.ZipUtils;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.ddh.gamsapi.TestUtilities.TestBag;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.TestUtilities.TestProject;

import java.io.File;
import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserPrincipalAuditorMappingIT extends IntegrationTest {


  @Autowired
  MockMvc mockMvc;

  File bagFile;

  @Autowired
  IDatastreamRepository datastreamRepository;

  @Autowired
  IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  IProjectRepository projectRepository;

  @BeforeAll
  public void setup() throws IOException {
    bagFile = TestBag.loadFile();
  }


  /**
   * Creates a Project as global admin -> then ingests as project admin role.
   * Checks if the object has the expected auditing data.
   * @throws Exception if the test fails
   */
  @Test
  public void objectHasExpectedAuditingData() throws Exception {



    final String TEST_PROJECT_URL = String.format("/api/v1/projects/%s", TestProject.PROJECT_ABBR.getValue());

    // global admin creates the project
    String testGlobalAdminAuthority = GAMSAPIAuthorities.getSuperAdmin();

    // first create a project
    mockMvc.perform(
        MockMvcRequestBuilders.put(TEST_PROJECT_URL)
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .with(SecurityMockMvcRequestPostProcessors
              .oidcLogin()
              .authorities(new SimpleGrantedAuthority(testGlobalAdminAuthority))
        )
    ).andExpect(status().isOk());

    // project admin creates object via ingest
    String testProjectAdminAuthority = GAMSAPIAuthorities.getProjectAdmin(TestProject.PROJECT_ABBR.getValue());

    // ingest endpoint for project
    final String TEST_PROJECT_INGEST_URL = String.format("%s/objects", TEST_PROJECT_URL);

    // data to be send as multipart
    byte[] zippedBag = ZipUtils.zipDir(bagFile);
    MockPart mockPart = new MockPart(IngestStatics.FORM_PART_NAME.name, "test.zip", zippedBag);
    // add content type to the part
    mockPart.getHeaders().setContentType(MediaType.APPLICATION_OCTET_STREAM);

    mockMvc
        .perform(
            multipart(TEST_PROJECT_INGEST_URL)
                .part(mockPart)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                // there is no need to mock the user as oauth2 user.
                .with(SecurityMockMvcRequestPostProcessors
                    .oidcLogin()
                    .authorities(new SimpleGrantedAuthority(testProjectAdminAuthority))
                )
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        )
        .andExpect(status().isOk());

    // provided by the oidcLogin() method
    final String TEST_USERNAME = "user";

    DigitalObject digitalObject =
        digitalObjectRepository.findById(TestDigitalObject.DIGITAL_OBJECT_ID.getValue()).orElseThrow();

    String createdBy = digitalObject.getCreatedBy();
    String modifiedBy = digitalObject.getModifiedBy();

    Assertions.assertThat(createdBy).isNotNull();
    Assertions.assertThat(createdBy).isEqualTo(TEST_USERNAME);
    Assertions.assertThat(modifiedBy).isNotNull();
    Assertions.assertThat(modifiedBy).isEqualTo(TEST_USERNAME);

    // additional checks that ingest created some stuff
    Assertions.assertThat(datastreamRepository.findAll()).isNotEmpty();
    Assertions.assertThat(digitalObjectRepository.findAll()).isNotEmpty();

  }



}
