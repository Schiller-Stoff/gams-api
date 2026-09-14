package org.ddh.gamsapi.domain.ArchivalRecord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.IHandleClient;
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
  private final IHandleClient handleClient;

  @Override
  public List<ArchivalRecordCompactView> findForObject(String digitalObjectId) {

    if(!digitalObjectRepository.existsById(digitalObjectId)){
      throw new DigitalObjectNotFoundException(
          "Cannot find archival records for digital object: " +  digitalObjectId + " The digital object does not exist."
      );
    }

    return archivalRecordRepository.findAllByDigitalObjectIdOrderByPublicationTimeStampDesc(digitalObjectId);
  }


  @Override
  @Transactional
  public void deleteById(String archivalRecordPid) {
    if(!archivalRecordRepository.existsById(archivalRecordPid)){
      throw new ArchivalRecordNotFoundException(
          "Cannot delete archival record with pid: " + archivalRecordPid + " The archival record does not exist."
      );
    }
    archivalRecordRepository.deleteById(archivalRecordPid);
  }

  @Override
  @Transactional
  public ArchivalRecord createArchivalRecordByObjectId(String objectId) {
    if(!digitalObjectRepository.existsById(objectId)){
      throw new DigitalObjectNotFoundException(
          "Cannot create archival record for digital object: " + objectId + " The digital object does not exist."
      );
    }

    ArchivalRecord archivalRecord = new ArchivalRecord();
    String pid = handleClient.generate();
    archivalRecord.setPid(pid);
    DigitalObject linkedDigitalObject = new DigitalObject();
    linkedDigitalObject.setId(objectId);
    archivalRecord.setDigitalObject(linkedDigitalObject);

    var savedRecord = archivalRecordRepository.save(archivalRecord);
    log.info("Successfully created archival record with generated pid: {} for digital object: {}", archivalRecord, objectId);

    return savedRecord;
  }

  @Override
  public ArchivalRecord createArchivalRecordByObjectIdAndPid(String objectId, String pid) {
    if(!digitalObjectRepository.existsById(objectId)){
      throw new DigitalObjectNotFoundException(
          "Cannot create archival record for digital object: " + objectId + " The digital object does not exist."
      );
    }

    if(archivalRecordRepository.existsById(pid)){
      throw new ArchivalRecordAlreadyExistsException(
          "Cannot create archival record for digital object: " + objectId + ". The archival record already exists with handle: " + pid
      );
    }

    ArchivalRecord archivalRecord = new ArchivalRecord();
    archivalRecord.setPid(pid);

    DigitalObject linkedDigitalObject = new DigitalObject();
    linkedDigitalObject.setId(objectId);
    archivalRecord.setDigitalObject(linkedDigitalObject);

    var savedRecord = archivalRecordRepository.save(archivalRecord);
    log.info("Successfully created archival record {} for digital object: {}", archivalRecord, objectId);

    return savedRecord;
  }

  @Override
  public ArchivalRecord createArchivalRecord() {
    ArchivalRecord archivalRecord = new ArchivalRecord();
    String pid = handleClient.generate();
    archivalRecord.setPid(pid);

    var savedRecord = archivalRecordRepository.save(archivalRecord);
    log.info("Successfully created archival record {} (without assigned object or pid)", archivalRecord);
    return savedRecord;
  }

  @Override
  public ArchivalRecord createArchivalRecordByPid(String pid) {
    if(archivalRecordRepository.existsById(pid)){
      throw new ArchivalRecordAlreadyExistsException(
          "Cannot create archival record. Archival record with pid already exists: " + pid
      );
    }

    ArchivalRecord archivalRecord = new ArchivalRecord();
    archivalRecord.setPid(pid);

    var savedRecord = archivalRecordRepository.save(archivalRecord);
    log.info("Successfully created archival record {}", archivalRecord);
    return savedRecord;

  }

  @Override
  public void saveArchivalRecord(ArchivalRecordDto archivalRecord) {

    // TODO think about

  }

}
