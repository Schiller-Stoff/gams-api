package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArchivalRecordService implements IArchivalRecordService {

  private final IArchivalRecordRepository archivalRecordRepository;
  private final IDigitalObjectRepository digitalObjectRepository;

  @Override
  public List<ArchivalRecordCompactView> findForObject(String digitalObjectId) {
    return archivalRecordRepository.findAllByDigitalObjectIdOrderByTimeStampDesc(digitalObjectId);
  }

  @Override
  @Transactional
  public ArchivalRecord reserve(String objectId, ArchivalRecordReserveDto archivalRecordReserveDto) {

    if (!digitalObjectRepository.existsById(objectId)) {
      throw new DigitalObjectNotFoundException(
          "Cannot save archival record " + archivalRecordReserveDto + ". The digital object with id does not exist: " + objectId
      );
    }

    if (archivalRecordRepository.existsByDigitalObjectIdAndArchivingStatusIn(objectId, ArchivingStatus.getBlockingStatuses())) {
      throw new ArchivalRecordAlreadyActiveException(
          "Cannot create archival record for digital object " + objectId
              + ". An archival record with status DRAFTED or RESERVED already exists."
      );
    }

    ArchivalRecord archivalRecord = new ArchivalRecord();
    archivalRecord.setPid(archivalRecordReserveDto.getPid());
    archivalRecord.setTimeStamp(Instant.now());

    //
    archivalRecord.setArchivingStatus(ArchivingStatus.RESERVED);

    // link to digital object
    DigitalObject digitalObject = new DigitalObject();
    digitalObject.setId(objectId);
    archivalRecord.setDigitalObject(digitalObject);



    return archivalRecordRepository.save(archivalRecord);
  }

  @Override
  public List<ArchivalRecordCompactView> findForObjectByArchivingStatus(String digitalObjectId, ArchivingStatus archivingStatus) {
    return archivalRecordRepository.findArchivalRecordsByDigitalObjectIdAndArchivingStatus(digitalObjectId, archivingStatus);
  }

  @Override
  public void deleteById(Long archivalRecordId) {
    archivalRecordRepository.deleteById(archivalRecordId);
  }

  @Override
  @Transactional
  public void draftArchivalRecord(String objectId, ArchivalRecordDraftDto archivalRecordDraftDto) {

    // this might not be necessary
    if (!digitalObjectRepository.existsById(objectId)) {
      throw new DigitalObjectNotFoundException(
          "Cannot update archival record " + archivalRecordDraftDto + ". The digital object with id does not exist: " + objectId
      );
    }

    var foundArchivalRecordOptional = archivalRecordRepository.findByDigitalObjectIdAndArchivingStatusIn(
        objectId,
        EnumSet.of(ArchivingStatus.RESERVED)
    );

    var foundArchivalRecord = foundArchivalRecordOptional.orElseThrow(() ->
      new ArchivalRecordNoActiveRecordException(
          "Cannot draft archival record. No reserved archival record available: " + archivalRecordDraftDto + ". For object with id: " +  objectId
      )
    );

    foundArchivalRecord.setPid(archivalRecordDraftDto.getPid());
    foundArchivalRecord.setTimeStamp(archivalRecordDraftDto.getTimeStamp());
    foundArchivalRecord.setExternalId(archivalRecordDraftDto.getExternalId());
    foundArchivalRecord.setArchivingStatus(ArchivingStatus.DRAFTED);

    log.info("Successfully drafted archival record {} for digital object {}", archivalRecordDraftDto,  objectId);

  }

  @Override
  @Transactional
  public void publishArchivalRecord(String objectId, ArchivalRecordPublishDto archivalRecordPublishDto) {

    // this might not be necessary
    if (!digitalObjectRepository.existsById(objectId)) {
      throw new DigitalObjectNotFoundException(
          "Cannot publish archival record " + archivalRecordPublishDto + ". The digital object with id does not exist: " + objectId
      );
    }

    var foundArchivalRecordOptional = archivalRecordRepository.findByDigitalObjectIdAndArchivingStatusIn(
        objectId,
        EnumSet.of(ArchivingStatus.DRAFTED)
    );

    var foundArchivalRecord = foundArchivalRecordOptional.orElseThrow(() ->
        new ArchivalRecordNoActiveRecordException(
            "Cannot publish archival record. No drafted archival record available: " + archivalRecordPublishDto + ". For object with id: " +  objectId
        )
    );

    foundArchivalRecord.setTimeStamp(archivalRecordPublishDto.getTimeStamp());
    foundArchivalRecord.setArchivingStatus(ArchivingStatus.PUBLISHED);
  }
}
