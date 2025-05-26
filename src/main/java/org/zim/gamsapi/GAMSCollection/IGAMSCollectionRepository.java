package org.zim.gamsapi.GAMSCollection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.GAMSCollection.interfaces.GAMSCollectionDetailsView;
import org.zim.gamsapi.GAMSCollection.interfaces.GamsCollectionCompactView;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import java.util.List;
import java.util.Optional;

public interface IGAMSCollectionRepository extends CrudRepository<GAMSCollection, String> {

  Optional<GAMSCollectionDetailsView> findProjectedById(String id);

  Page<GamsCollectionCompactView> findByDigitalObjectsId(String id, Pageable pageable);

  Page<GamsCollectionCompactView> findAllProjectedBy(Pageable pageable);

  List<GAMSCollection> findByProject_ProjectAbbr(String projectAbbr);
  Page<GamsCollectionCompactView> findByProject_ProjectAbbr(String projectAbbr, Pageable pageable);
  List<GAMSCollection> findByDigitalObjectsContaining(DigitalObject digitalObject);

}
