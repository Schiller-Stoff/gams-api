package org.ddh.gamsapi.TestUtilities;

import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.EventCaptureListener;
import org.ddh.gamsapi.domain.Datastream.DatastreamContent.DatastreamContentRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord.IArchivalRecordRepository;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.ddh.gamsapi.domain.DigitalObject.SubmissionRecord.ISubmissionRecordRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class TestCleanupService {

  private final DatastreamContentRepository datastreamContentRepository;

  @Autowired
  private EventCaptureListener eventCaptureListener;

  @Autowired
  private IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  private IDublinCoreEntryRepository dublinCoreElementRepository;

  @Autowired
  private IDatastreamRepository datastreamRepository;
  @Autowired
  private IProjectRepository projectRepository;

  @Autowired
  private ISubmissionRecordRepository submissionRecordRepository;

  @Autowired
  private IArchivalRecordRepository archivalRecordRepository;

  public TestCleanupService(DatastreamContentRepository datastreamContentRepository) {
    this.datastreamContentRepository = datastreamContentRepository;
  }

  /**
   * Main method cleans up all test data using the repository layer of the
   * gams-api.
   */
  @Transactional
  public void cleanup(){
    eventCaptureListener.clearEvents();
    datastreamContentRepository.deleteAll();
    dublinCoreElementRepository.deleteAll();
    datastreamRepository.deleteAll();
    submissionRecordRepository.deleteAll();
    archivalRecordRepository.deleteAll();
    digitalObjectRepository.deleteAll();
    projectRepository.deleteAll();
  }

}


