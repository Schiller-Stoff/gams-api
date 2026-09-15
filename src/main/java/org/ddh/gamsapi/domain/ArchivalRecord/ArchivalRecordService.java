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
  @Transactional
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
  @Transactional
  public ArchivalRecord createArchivalRecord() {
    ArchivalRecord archivalRecord = new ArchivalRecord();
    String pid = handleClient.generate();
    archivalRecord.setPid(pid);

    var savedRecord = archivalRecordRepository.save(archivalRecord);
    log.info("Successfully created archival record {} (without assigned object or pid)", archivalRecord);
    return savedRecord;
  }

  @Override
  @Transactional
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
  @Transactional
  public ArchivalRecord saveArchivalRecord(ArchivalRecordDto archivalRecord) {
    if((archivalRecord.getPublicationTimeStamp() == null) &&  (archivalRecord.getExternalId() == null)){
      throw new ArchivalRecordInvalidStateException(
          "Cannot patch archival record. You either have to provide a publication timestamp or an external id - but both are null. Got values: " + archivalRecord
      );
    }

    var curArchivalRecord = archivalRecordRepository.findById(archivalRecord.getPid())
        .orElseThrow(() ->
          new ArchivalRecordNotFoundException(
              "Cannot patch archival record. Archival record with pid does not exist: " + archivalRecord.getPid()
          ));

    curArchivalRecord.setPublicationTimeStamp(archivalRecord.getPublicationTimeStamp());
    curArchivalRecord.setExternalId(archivalRecord.getExternalId());
    // digital object cannot be changed (after creation)
    // curArchivalRecord.setDigitalObject();
    // pid cannot be changed after creation!
    // curArchivalRecord.setPid(archivalRecord.getPid());

    if(archivalRecord.getPublicationTimeStamp() != null){
      if(archivalRecord.getExternalId() == null){
        throw new ArchivalRecordInvalidStateException(
            "Cannot patch archival record. Archival record has a valid publicationTimeStamp but no external id assigned to it - which is illegal." + curArchivalRecord
        );
      }

    }

    log.info("Successfully patched archival record {}", curArchivalRecord);

    return curArchivalRecord;

  }
}
