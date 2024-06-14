package org.zim.gamsapi.System.security;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockPart;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.Ingest.utils.IngestStatics;
import org.zim.gamsapi.Ingest.utils.ZipUtils;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestBag;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestProject;

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
    projectRepository.save(Project.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build());
  }

  @AfterAll
  public void tearDown(){
    datastreamRepository.deleteAll();
    digitalObjectRepository.deleteAll();
    projectRepository.deleteAll();
  }


  @Test
  public void objectHasExpectedAuditingData() throws Exception {

    byte[] zippedBag = ZipUtils.zipDir(bagFile);
    MockPart mockPart = new MockPart(IngestStatics.FORM_PART_NAME.name, "test.zip", zippedBag);

    String testProjectAdminAuthority = GAMSAPIAuthorities.getProjectAdmin(TestProject.PROJECT_ABBR.getValue());

    mockMvc
        .perform(
            multipart("/api/v1/projects/{projectAbbr}/objects", TestProject.PROJECT_ABBR.getValue())
                .part(mockPart)
                // there is no need to mock the user as oauth2 user.
                .with(SecurityMockMvcRequestPostProcessors
                    .oidcLogin()
                    .authorities(new SimpleGrantedAuthority(testProjectAdminAuthority))
                )
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

    // cleanup
    datastreamRepository.deleteAll();
    digitalObjectRepository.deleteAll();
  }



}
