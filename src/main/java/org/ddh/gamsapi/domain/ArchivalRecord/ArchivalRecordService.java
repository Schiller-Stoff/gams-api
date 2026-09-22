package org.ddh.gamsapi.domain.ArchivalRecord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.IHandleClient;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.dto.ArchivalRecordDraftDto;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.dto.ArchivalRecordPublishDto;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.exceptions.ArchivalRecordInconsistentActiveRecordsException;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.infrastructure.System.dto.PagedResponse;
import org.springframework.data.domain.Pageable;
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
  public PagedResponse<ArchivalRecordCompactView> findActiveArchivalRecordForObject(String objectId) {
    if(!digitalObjectRepository.existsById(objectId)){
      throw new DigitalObjectNotFoundException(
          "Cannot find archival records for digital object: " +  objectId + " The digital object does not exist."
      );
    }

    var activeRecords = archivalRecordRepository.findActiveArchivalRecordsByDigitalObjectIdAndArchivalStateIn(
        objectId,
        ArchivalState.getBlockingStatuses(),
        Pageable.unpaged()
    );

    if(activeRecords.isEmpty()){
      throw new ArchivalRecordNoActiveRecordException(
          "Cannot find any active archival record for digital object: " +  objectId + " No active records found."
      );
    }

    if(activeRecords.getTotalElements() > 1){
      throw new ArchivalRecordInconsistentActiveRecordsException(
          "Cannot retrieve active archival record for object " + objectId + " -  Active archival record count is unexpectedly " + activeRecords.getTotalElements() + " Only one should be active."
      );
    }

    log.info("Successfully found active archival record {}", activeRecords.getContent().getFirst());
    return PagedResponse.from(
        activeRecords
    );

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
  public ArchivalRecord reserveArchivalRecordByObjectId(String objectId) {
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
  public ArchivalRecord reserveArchivalRecordByObjectIdAndPid(String objectId, String pid) {
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
  public ArchivalRecord reserveArchivalRecord() {
    ArchivalRecord archivalRecord = new ArchivalRecord();
    String pid = handleClient.generate();
    archivalRecord.setPid(pid);
    archivalRecord.setArchivalState(ArchivalState.RESERVED);

    var savedRecord = archivalRecordRepository.save(archivalRecord);
    log.info("Successfully created archival record {} (without assigned object or pid)", archivalRecord);
    return savedRecord;
  }

  @Override
  @Transactional
  public ArchivalRecord reserveArchivalRecordByPid(String pid) {
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
  public ArchivalRecord updateArchivalRecord(ArchivalRecordDto archivalRecord) {

    var curArchivalRecord = archivalRecordRepository.findById(archivalRecord.getPid())
        .orElseThrow(() ->
          new ArchivalRecordNotFoundException(
              "Cannot patch archival record. Archival record with pid does not exist: " + archivalRecord.getPid()
          ));

    // don't change the pid
    curArchivalRecord.setArchivalState(archivalRecord.getArchivalState());
    curArchivalRecord.setExternalId(archivalRecord.getExternalId());
    curArchivalRecord.setPublicationTimeStamp(archivalRecord.getPublicationTimeStamp());

    if(archivalRecord.getObjectId() != null){
      // check if defined digital object exists.
      if(!digitalObjectRepository.existsById(archivalRecord.getPid())){
        throw new DigitalObjectNotFoundException(
            "Cannot change archival record: The digital object with id " + archivalRecord.getObjectId() + " does not exist. For archival record change dto: " + archivalRecord
        );
      }
      // link digital object if given in dto
      DigitalObject linkedDigitalObject = new DigitalObject();
      linkedDigitalObject.setId(archivalRecord.getObjectId());
      curArchivalRecord.setDigitalObject(linkedDigitalObject);
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
    activeRecord.setArchivalState(ArchivalState.DRAFT);
    return activeRecord;
  }

  @Override
  @Transactional
  public ArchivalRecord publishArchivalRecord(String pid, ArchivalRecordPublishDto archivalRecordPublishDto) {
    if(archivalRecordPublishDto.getPublicationTimeStamp() == null){
      throw new ArchivalRecordInvalidPublicationTimeStampException(
          "Cannot publish archival record - the provided publication timestamp is null. Got dto: " + archivalRecordPublishDto
      );
    }

    var activeRecord = archivalRecordRepository.findById(pid)
        .orElseThrow( () -> new ArchivalRecordNotFoundException(
            "Cannot publish archival record with pid: " + pid + " The record does not exist."
        ));

    if(activeRecord.getArchivalState() != ArchivalState.DRAFT){
      throw new ArchivalRecordInvalidStateException(
          "Cannot publish archival record - Requested archival record has not the required DRAFT state. For pid: " + pid + " And archival record: " + archivalRecordPublishDto
      );
    }

    activeRecord.setPublicationTimeStamp(archivalRecordPublishDto.getPublicationTimeStamp());
    activeRecord.setArchivalState(ArchivalState.PUBLISHED);
    return activeRecord;
  }
}
