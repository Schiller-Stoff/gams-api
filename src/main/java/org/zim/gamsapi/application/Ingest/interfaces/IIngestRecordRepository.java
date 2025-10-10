package org.zim.gamsapi.application.Ingest.interfaces;

import org.springframework.data.repository.CrudRepository;
import org.zim.gamsapi.application.Ingest.IngestRecord;

public interface IIngestRecordRepository extends CrudRepository<IngestRecord, String> {


}
