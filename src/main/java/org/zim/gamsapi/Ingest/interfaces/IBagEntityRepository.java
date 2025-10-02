package org.zim.gamsapi.Ingest.interfaces;

import org.springframework.data.repository.CrudRepository;
import org.zim.gamsapi.Ingest.BagEntity;

public interface IBagEntityRepository extends CrudRepository<BagEntity, String> {


}
