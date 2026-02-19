package org.ddh.gamsapi.application.Ingest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Ingest.exceptions.IngestObjectAlreadyExistsException;
import org.ddh.gamsapi.domain.Datastream.Datastream;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObjectCreatedEvent;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.DublinCoreEntry;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.ddh.gamsapi.domain.DigitalObject.SubmissionRecord.ISubmissionRecordRepository;
import org.ddh.gamsapi.domain.DigitalObject.SubmissionRecord.SubmissionRecord;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.application.Ingest.utils.Bagit.Bag;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectNotFoundException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Handles strictly transactional database operations for Ingest.
 * Separated from IngestService to avoid self-injection and Native Image proxy issues.
 * (keeping performance intense IO operations separate from database transactional operations)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IngestPersistenceService {

  private final IProjectRepository projectRepository;
  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;
  private final IDublinCoreEntryRepository dublinCoreElementRepository;
  private final ISubmissionRecordRepository submissionRecordRepository;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final BuildProperties buildProperties;

  @Transactional(rollbackFor = Exception.class)
  public void persistIngest(String projectAbbr,
                            DigitalObject digitalObject,
                            List<Datastream> datastreams,
                            List<DublinCoreEntry> dublinCoreEntries,
                            Bag bag) {

    var foundProject = projectRepository.findById(projectAbbr)
        .orElseThrow(() -> new ProjectNotFoundException(
            "Project does not exist: " + projectAbbr
        ));

    // Double check existence (optimistic locking pattern) in case of race condition
    // abort ingest if digital object already exists
    if (digitalObjectRepository.existsById(digitalObject.getId())) {
      throw new IngestObjectAlreadyExistsException(
          "Cannot ingest object with id " + digitalObject.getId() + ". Digital object already exists and must be deleted before another ingest process. Ingest against project: " + projectAbbr
      );
    }

    // 1. Save Digital Object
    final DigitalObject savedObject = digitalObjectRepository.save(digitalObject);
    log.debug("Successfully saved digital object: {} for project {}", digitalObject, projectAbbr);

    // logic to save the related submission record
    var submissionRecord = SubmissionRecord.builder()
        .digitalObject(savedObject)
        .createdBy(bag.getBagData().getCreatedBy())
        .source(bag.getBagData().getSource())
        .schema(bag.getBagData().getSchema())
        .contactMail(bag.getBagInfo().getContactMail())
        .baggingDate(bag.getBagInfo().getDate())
        .externalDescription(bag.getBagInfo().getExternalDescription())
        .payloadOxum(bag.getBagInfo().getPayloadOxum())
        .bagVersion(bag.getBagMeta().getBagItVersion())
        .tagFileCharacterEncoding(bag.getBagMeta().getTagFileCharacterEncoding())
        .gamsApiVersion(buildProperties.getVersion())
        .build();

    submissionRecordRepository.save(submissionRecord);
    log.debug("Successfully saved bag entity: {} for project {}", submissionRecord, projectAbbr);

    // 3. Save Datastreams (Metadata only)
    for (Datastream ds : datastreams) {
      ds.setDigitalObject(savedObject); // Re-attach managed entity
      datastreamRepository.save(ds);
    }

    // 4. save dublin core
    for (DublinCoreEntry dc : dublinCoreEntries) {
      dc.setDigitalObject(savedObject); // Re-attach managed entity
      dublinCoreElementRepository.save(dc);
    }

    // publish creation event
    // TODO missing auditing info
    applicationEventPublisher.publishEvent(
        new DigitalObjectCreatedEvent(this, savedObject)
    );

    log.debug("Successfully persisted object {} for project {}", savedObject.getId(), projectAbbr);
  }
}