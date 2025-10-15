package org.ddh.gamsapi.domain.DigitalObject.SubmissionRecord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;


@Service
@Slf4j
@RequiredArgsConstructor
public class SubmissionRecordService implements  ISubmissionRecordService {

  private final ISubmissionRecordRepository submissionRecordRepository;

  public SubmissionRecord find(String digitalObjectId) {
    return submissionRecordRepository.findById(digitalObjectId).orElseThrow( () -> {
      String msg = "SubmissionRecord not found for DigitalObject ID: " + digitalObjectId;
      log.error(msg);
      // TODO better error msg
      return new DigitalObjectNotFoundException(msg);
    });

  }

}
