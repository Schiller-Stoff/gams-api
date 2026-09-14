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

}
