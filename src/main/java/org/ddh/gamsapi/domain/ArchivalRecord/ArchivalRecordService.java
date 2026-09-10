package org.ddh.gamsapi.domain.ArchivalRecord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArchivalRecordService implements IArchivalRecordService {

  private final IArchivalRecordRepository archivalRecordRepository;
  private final IDigitalObjectRepository digitalObjectRepository;

  @Override
  public List<ArchivalRecordCompactView> findForObject(String digitalObjectId) {
    return archivalRecordRepository.findAllByDigitalObjectIdOrderByPublicationTimeStampDesc(digitalObjectId);
  }

  @Override
  @Transactional
  public ArchivalRecord reserve(String objectId, ArchivalRecordReserveDto archivalRecordReserveDto) {

    if (!digitalObjectRepository.existsById(objectId)) {
      throw new DigitalObjectNotFoundException(
          "Cannot save archival record " + archivalRecordReserveDto + ". The digital object with id does not exist: " + objectId
      );
    }

    ArchivalRecord archivalRecord = new ArchivalRecord();
    archivalRecord.setPid(archivalRecordReserveDto.getPid());

    // link to digital object
    DigitalObject digitalObject = new DigitalObject();
    digitalObject.setId(objectId);
    archivalRecord.setDigitalObject(digitalObject);



    return archivalRecordRepository.save(archivalRecord);
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

  }
}
