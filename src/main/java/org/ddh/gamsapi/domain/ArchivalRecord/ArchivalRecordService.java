package org.ddh.gamsapi.domain.ArchivalRecord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.IHandleClient;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.dto.ArchivalRecordDraftDto;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.dto.ArchivalRecordPublishDto;
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

    if(archivalRecordRepository.existsByDigitalObjectIdAndArchivalStateIn(objectId, ArchivalState.getBlockingStatuses())) {
      throw new ArchivalRecordAlreadyActiveException(
          "Cannot create archival record for digital object " + objectId
              + ". An archival record with blocking status already exists."
      );
    }

    ArchivalRecord archivalRecord = new ArchivalRecord();
    archivalRecord.setArchivalState(ArchivalState.RESERVED);

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
    archivalRecord.setArchivalState(ArchivalState.RESERVED);
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
    // TODO hide method atm?
    ArchivalRecord archivalRecord = new ArchivalRecord();
    // TODO add handle server communication
    String pid = handleClient.generate();
    archivalRecord.setPid(pid);
    archivalRecord.setArchivalState(ArchivalState.RESERVED);

    var savedRecord = archivalRecordRepository.save(archivalRecord);
    log.info("Successfully created archival record {} (without assigned object or pid)", archivalRecord);
    return savedRecord;
  }

  @Override
  @Transactional
  public ArchivalRecord createArchivalRecordByPid(String pid) {
    // TODO hide method atm?
    if(archivalRecordRepository.existsById(pid)){
      throw new ArchivalRecordAlreadyExistsException(
          "Cannot create archival record. Archival record with pid already exists: " + pid
      );
    }

    ArchivalRecord archivalRecord = new ArchivalRecord();
    archivalRecord.setPid(pid);
    archivalRecord.setArchivalState(ArchivalState.RESERVED);

    var savedRecord = archivalRecordRepository.save(archivalRecord);
    log.info("Successfully created archival record {}", archivalRecord);
    return savedRecord;

  }

  @Override
  @Transactional
  public ArchivalRecord saveArchivalRecord(ArchivalRecordDto archivalRecord) {

    // TODO might need to remove this method completely

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

    // cannot set publication timestamp to null if once set
    if(archivalRecord.getPublicationTimeStamp() != null){
      curArchivalRecord.setPublicationTimeStamp(archivalRecord.getPublicationTimeStamp());
    }

    // cannot set archival record to null if once set
    if(archivalRecord.getExternalId() != null){
      curArchivalRecord.setExternalId(archivalRecord.getExternalId());
    }

    // digital object cannot be changed (after creation)
    // pid cannot be changed after creation!

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


  @Override
  @Transactional
  public ArchivalRecord draftArchivalRecord(String pid, ArchivalRecordDraftDto archivalRecordDraftDto) {

    if(archivalRecordDraftDto.getExternalId() == null){
      throw new ArchivalRecordInvalidStateException(
          "Cannot draft archival record - the provided external id is null. Got dto: " + archivalRecordDraftDto
      );
    }

    var activeRecord = archivalRecordRepository.findById(pid)
        .orElseThrow( () -> new ArchivalRecordNotFoundException(
            "Cannot draft archival record with pid: " + pid + " The record does not exist."
        ));

    if(activeRecord.getArchivalState() != ArchivalState.RESERVED){
      throw new ArchivalRecordInvalidStateException(
          "Cannot draft archival record with pid " + pid + ". The requested record has not the required state " + ArchivalState.RESERVED + " Got actual state: " + activeRecord.getArchivalState()
      );
    }

    activeRecord.setExternalId(archivalRecordDraftDto.getExternalId());
    return activeRecord;
  }

  @Override
  @Transactional
  public ArchivalRecord publishArchivalRecord(String pid, ArchivalRecordPublishDto archivalRecordPublishDto) {
    // TODO test

    if(archivalRecordPublishDto.getPublicationTimeStamp() == null){
      throw new ArchivalRecordInvalidStateException(
          "Cannot publish archival record - the provided publication timestamp is null. Got dto: " + archivalRecordPublishDto
      );
    }

    var activeRecord = archivalRecordRepository.findById(pid)
        .orElseThrow( () -> new ArchivalRecordNotFoundException(
            "Cannot publish archival record with pid: " + pid + " The record does not exist."
        ));

    // TODO validate? (external id must be set at this moment)

    activeRecord.setPublicationTimeStamp(archivalRecordPublishDto.getPublicationTimeStamp());
    return activeRecord;
  }
}
