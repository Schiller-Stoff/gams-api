package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.springframework.stereotype.Service;

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
  public ArchivalRecord reserve(ArchivalRecordReserveDto archivalRecordReserveDto) {

    final String DIGITAL_OBJECT_ID = archivalRecordReserveDto.getDigitalObjectId();

    if (!digitalObjectRepository.existsById(DIGITAL_OBJECT_ID)) {
      throw new DigitalObjectNotFoundException(
          "Cannot save archival record " + archivalRecordReserveDto + ". The digital object with id does not exist: " + DIGITAL_OBJECT_ID
      );
    }

    ArchivalRecord archivalRecord = new ArchivalRecord();
    archivalRecord.setPid(archivalRecordReserveDto.getPid());
    archivalRecord.setTimeStamp(archivalRecordReserveDto.getTimeStamp());
    archivalRecord.setExternalId(archivalRecordReserveDto.getExternalId());

    //
    archivalRecord.setArchivingStatus(ArchivingStatus.RESERVED);

    // link to digital object
    DigitalObject digitalObject = new DigitalObject();
    digitalObject.setId(DIGITAL_OBJECT_ID);
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
  public void updateArchivalRecord(ArchivalRecordUpdateDto archivalRecordUpdateDto) {


    // TODO implement!
    throw new NotImplementedException("NOT IMPLEMENTED CURRENTLY");



  }
}
