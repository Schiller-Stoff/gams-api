package org.zim.gamsapi.Ingest;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.Ingest.utils.ZipUtils;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestProject;

import java.io.File;
import java.io.IOException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IngestServiceIT extends IntegrationTest {

  private final String TEST_BAG_LOCATION = "testfiles/ingest/test-bag";

  @Autowired
  IProjectRepository projectRepository;

  @Autowired
  IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  IDatastreamRepository datastreamRepository;

  @Autowired
  IngestService ingestService;

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
    // everything should be removed
    Assertions.assertThat(projectRepository.findAll()).isEmpty();
  }

  @Test
  public void createsExpectedDigitalObject(){

    byte[] zippedBag = ZipUtils.zipDir(bagFile);

    Ingest ingest = new Ingest();
    ingest.setZippedBagItFolder(zippedBag);
    ingest.setProjectAbbr(TestProject.PROJECT_ABBR.getValue());

    ingestService.ingest(ingest);

    // assert that the digital object was created
    Assertions.assertThat(digitalObjectRepository.findAll()).isNotEmpty();
    Assertions.assertThat(datastreamRepository.findAll())
        .isNotEmpty()
        .hasSize(3);

    // cleanup
    datastreamRepository.deleteAll();
    digitalObjectRepository.deleteAll();

  }




}
