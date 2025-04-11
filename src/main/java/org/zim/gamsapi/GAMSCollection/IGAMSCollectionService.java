package org.zim.gamsapi.GAMSCollection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.GAMSCollection.interfaces.GAMSCollectionDetailsView;
import org.zim.gamsapi.GAMSCollection.interfaces.GamsCollectionCompactView;

import java.util.List;

public interface IGAMSCollectionService {
  void save(GAMSCollection GAMSCollection);
  void delete(GAMSCollection GAMSCollection);
  GAMSCollectionDetailsView findById(String id);
  Page<GamsCollectionCompactView> findAll(Pageable pageable);
  List<GAMSCollection> findByProjectAbbr(String projectAbbr);
  Page<GamsCollectionCompactView> findByProjectAbbr(String projectAbbr, Pageable pageable);
  GAMSCollection addDigitalObjectToCollection(String collectionId, String digitalObjectId);
  GAMSCollection removeDigitalObjectFromCollection(String collectionId, String digitalObjectId);
  Page<GamsCollectionCompactView> findByDigitalObject(String digitalObjectId, Pageable pageable);
  Page<DigitalObjectListItemView> findDigitalObjectsByCollectionId(String collectionId, Pageable pageable);
}
