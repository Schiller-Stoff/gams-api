package org.zim.gamsapi.Ingest;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.MockMvc;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.Ingest.utils.IngestStatics;
import org.zim.gamsapi.Ingest.utils.ZipUtils;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestBag;
import org.zim.gamsapi.enums.TestProject;

import java.io.File;
import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false) // deactivates security filters
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

  @Autowired
  IDatastreamContentRepository datastreamContentRepository;

  // disables auditing
  @MockBean
  private AuditingHandler auditingHandler;


  File bagFile;

  @BeforeEach
  public void setup() throws IOException {
    bagFile = TestBag.loadFile();
    projectRepository.save(Project.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build());
  }

  @Test
  public void ingestCreatesAtLeastOneObjectAndOneDatastream() throws Exception {
    byte[] zippedBag = ZipUtils.zipDir(bagFile);
    MockPart mockPart = new MockPart(IngestStatics.FORM_PART_NAME.name, "test.zip", zippedBag);
    mockMvc
        .perform(
            multipart("/api/v1/projects/{projectAbbr}/objects", TestProject.PROJECT_ABBR.getValue())
            .part(mockPart)
        )
        .andExpect(status().isOk());


    Assertions.assertThat(datastreamRepository.findAll()).isNotEmpty();
    Assertions.assertThat(digitalObjectRepository.findAll()).isNotEmpty();

  }
}