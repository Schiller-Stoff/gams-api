package org.zim.gamsapi.application.Ingest.interfaces;

import org.springframework.data.repository.CrudRepository;
import org.zim.gamsapi.application.Ingest.SubmissionRecord;

public interface ISubmissionRecordRepository extends CrudRepository<SubmissionRecord, String> {


}
