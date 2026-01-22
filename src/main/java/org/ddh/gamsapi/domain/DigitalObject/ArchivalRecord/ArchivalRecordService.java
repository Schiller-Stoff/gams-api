package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObjectBuilder;
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
  public ArchivalRecord save(ArchivalRecordCreateDto archivalRecordCreateDto) {

    final String DIGITAL_OBJECT_ID = archivalRecordCreateDto.getDigitalObjectId();

    if(!digitalObjectRepository.existsById(DIGITAL_OBJECT_ID)){
      throw new DigitalObjectNotFoundException(
          "Cannot save archival record " + archivalRecordCreateDto  + ". The digital object with id does not exist: " + DIGITAL_OBJECT_ID
      );
    }

    ArchivalRecord archivalRecord = new ArchivalRecord();
    archivalRecord.setPid(archivalRecordCreateDto.getPid());
    archivalRecord.setTimeStamp(archivalRecordCreateDto.getTimeStamp());

    DigitalObject digitalObject = new DigitalObject();
    digitalObject.setId(DIGITAL_OBJECT_ID);

    archivalRecord.setDigitalObject(digitalObject);
    return archivalRecordRepository.save(archivalRecord);
  }
}
