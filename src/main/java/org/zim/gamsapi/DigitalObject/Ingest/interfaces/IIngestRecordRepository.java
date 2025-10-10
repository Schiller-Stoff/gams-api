package org.zim.gamsapi.DigitalObject.Ingest.interfaces;

import org.springframework.data.repository.CrudRepository;
import org.zim.gamsapi.DigitalObject.Ingest.IngestRecord;

public interface IIngestRecordRepository extends CrudRepository<IngestRecord, String> {


}
