package org.ddh.gamsapi.domain.ArchivalRecord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.HandleGenerator;
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
  public ArchivalRecord createArchivalRecord(String objectId) {
    if(!digitalObjectRepository.existsById(objectId)){
      throw new DigitalObjectNotFoundException(
          "Cannot create archival record for digital object: " + objectId + " The digital object does not exist."
      );
    }

    ArchivalRecord archivalRecord = new ArchivalRecord();

    // TODO communication with handle server - needs to allow to set handle
    String pid = handleClient.generate();
    // versions are not expressed in the pid
    archivalRecord.setPid(pid);

    // TODO test if nothing changes on object after save (object should not be saved along)
    DigitalObject linkedDigitalObject = new DigitalObject();
    linkedDigitalObject.setId(objectId);
    archivalRecord.setDigitalObject(linkedDigitalObject);

    // these fields should be null
    archivalRecord.setExternalId(null);
    archivalRecord.setPublicationTimeStamp(null);

    var savedRecord = archivalRecordRepository.save(archivalRecord);
    log.info("Successfully created archival record {} for digital object: {}", archivalRecord, objectId);

    return savedRecord;
  }

  @Override
  public void saveArchivalRecord(ArchivalRecordDto archivalRecord) {

  }
}
