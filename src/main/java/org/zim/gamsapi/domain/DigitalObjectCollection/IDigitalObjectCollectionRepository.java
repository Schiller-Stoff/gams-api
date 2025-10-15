package org.zim.gamsapi.domain.DigitalObjectCollection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.zim.gamsapi.domain.DigitalObjectCollection.interfaces.DigitalObjectCollectionDetailsView;
import org.zim.gamsapi.domain.DigitalObjectCollection.interfaces.DigitalObjectCollectionCompactView;
import org.zim.gamsapi.domain.DigitalObject.DigitalObject;
import java.util.List;
import java.util.Optional;

public interface IDigitalObjectCollectionRepository extends CrudRepository<DigitalObjectCollection, String> {

  Optional<DigitalObjectCollectionDetailsView> findProjectedById(String id);

  Page<DigitalObjectCollectionCompactView> findByDigitalObjectsId(String id, Pageable pageable);

  Page<DigitalObjectCollectionCompactView> findAllProjectedBy(Pageable pageable);

  List<DigitalObjectCollection> findByProject_ProjectAbbr(String projectAbbr);
  Page<DigitalObjectCollectionCompactView> findByProject_ProjectAbbr(String projectAbbr, Pageable pageable);
  List<DigitalObjectCollection> findByDigitalObjectsContaining(DigitalObject digitalObject);

}
