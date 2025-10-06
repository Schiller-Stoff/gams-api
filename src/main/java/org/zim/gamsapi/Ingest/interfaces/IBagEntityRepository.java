package org.zim.gamsapi.Ingest.interfaces;

import org.springframework.data.repository.CrudRepository;
import org.zim.gamsapi.Ingest.IngestRecord;

public interface IBagEntityRepository extends CrudRepository<IngestRecord, String> {


}
