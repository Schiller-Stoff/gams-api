package org.zim.gamsapi.Ingest;

import org.junit.Ignore;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.Ingest.utils.ZipUtils;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestProject;
import java.io.File;
import java.io.IOException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IngestControllerIT extends IntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  IProjectRepository projectRepository;

  @Autowired
  IDatastreamRepository datastreamRepository;

  @Autowired
  IDigitalObjectRepository digitalObjectRepository;

  private final String TEST_BAG_LOCATION = "testfiles/ingest/test-bag";


  File bagFile;

  @BeforeAll
  public void setup() throws IOException {
    bagFile = new ClassPathResource(TEST_BAG_LOCATION).getFile();
    projectRepository.save(Project.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build());
  }

  @AfterAll
  public void tearDown(){
    datastreamRepository.deleteAll();
    digitalObjectRepository.deleteAll();
    projectRepository.deleteAll();
  }

  @Test
  @Disabled("Test is disabled because it is not working as expected. Fails at authentication! Authentication needs to be set up for testing!")
  public void testIngest() throws Exception {
    byte[] zippedBag = ZipUtils.zipDir(bagFile);
    MockMultipartFile file = new MockMultipartFile("subInfoPackZIP", "test.zip", "application/zip", zippedBag);

    mockMvc
        .perform(multipart("/api/v1/projects/{projectAbbr}/objects", TestProject.PROJECT_ABBR.getValue())
        .file(file))
        .andExpect(status().isOk());
  }
}