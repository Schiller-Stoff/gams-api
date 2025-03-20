package org.zim.gamsapi.Ingest;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.auditing.AuditingHandler;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntry;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.Ingest.utils.ZipUtils;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.ProjectBuilder;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestBag;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestProject;
import java.io.File;
import java.io.IOException;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IngestServiceIT extends IntegrationTest {

  @Autowired
  IProjectRepository projectRepository;

  @Autowired
  IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  IDatastreamRepository datastreamRepository;

  @Autowired
  IDatastreamContentRepository datastreamContentRepository;

  @Autowired
  IDublinCoreEntryRepository dublinCoreElementRepository;

  @Autowired
  IngestService ingestService;

  File bagFile;

  // disables auditing
  @MockBean
  private AuditingHandler auditingHandler;

  @BeforeEach
  public void setup() throws IOException {
    bagFile = TestBag.loadFile();
    projectRepository.save(ProjectBuilder.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build());

    // ingest the bag
    byte[] zippedBag = ZipUtils.zipDir(bagFile);
    Ingest ingest = new Ingest();
    ingest.setZippedBagItFolder(zippedBag);
    ingest.setProjectAbbr(TestProject.PROJECT_ABBR.getValue());
    ingestService.ingest(ingest);
  }

  @Test
  public void createsExpectedDigitalObject_withDatastreamsAndContentAndDublinCore(){

    // assert that the digital object was created
    Assertions.assertThat(digitalObjectRepository.findAll()).isNotEmpty();
    var datastreams = datastreamRepository.findAll();
    Assertions.assertThat(datastreams)
        .isNotEmpty()
        .hasSize(5);

    // assert that expected datastream content exists on the fileystem
    datastreams.forEach(datastream -> {
      Assertions.assertThat(datastreamContentRepository.exists(datastream.deriveDatastreamId())).isTrue();
    });

    // assert that some dublin core elements were created
    Assertions.assertThat(
        dublinCoreElementRepository.count()
    )
        .isNotNull()
        .isNotEqualTo(0)
        .isGreaterThan(2);

  }

  @Test
  public void createsExpectedDublinCoreEntryNamesForTestDigitalObject() {

    List<DublinCoreEntry> dublinCoreEntries = dublinCoreElementRepository.findByDigitalObject(TestDigitalObject.generate());

    Assertions.assertThat(dublinCoreEntries)
        .isNotEmpty();

    List<String> foundDcElementNames = dublinCoreEntries.stream().map(DublinCoreEntry::getName).toList();

    Assertions.assertThat(foundDcElementNames)
        .contains(
            "title",
            "relation",
            "creator",
            "contributor",
            "date",
            "format",
            "language",
            "publisher",
            "source",
            "subject",
            "type");
  }




}
