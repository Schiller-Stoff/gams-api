package org.ddh.gamsapi.domain.DigitalObject.SubmissionRecord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;

import java.util.Optional;


@Service
@Slf4j
@RequiredArgsConstructor
public class SubmissionRecordService implements  ISubmissionRecordService {

  private final ISubmissionRecordRepository submissionRecordRepository;

  public Optional<SubmissionRecord> find(String digitalObjectId) {
    return submissionRecordRepository.findById(digitalObjectId);
  }

}
