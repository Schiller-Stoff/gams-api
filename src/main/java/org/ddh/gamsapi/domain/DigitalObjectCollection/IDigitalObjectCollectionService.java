package org.ddh.gamsapi.domain.DigitalObjectCollection;

import org.springframework.data.domain.Pageable;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.DigitalObjectListItemView;
import org.ddh.gamsapi.domain.DigitalObjectCollection.interfaces.DigitalObjectCollectionDetailsView;
import org.ddh.gamsapi.domain.DigitalObjectCollection.interfaces.DigitalObjectCollectionCompactView;
import org.ddh.gamsapi.infrastructure.System.dto.PagedResponse;

import java.util.List;

public interface IDigitalObjectCollectionService {
  void save(DigitalObjectCollection DigitalObjectCollection);
  void delete(DigitalObjectCollection DigitalObjectCollection);
  DigitalObjectCollectionDetailsView findById(String id);
  PagedResponse<DigitalObjectCollectionCompactView> findAll(Pageable pageable);
  List<DigitalObjectCollection> findByProjectAbbr(String projectAbbr);
  PagedResponse<DigitalObjectCollectionCompactView> findByProjectAbbr(String projectAbbr, Pageable pageable);
  void addDigitalObjectToCollection(String collectionId, String digitalObjectId);

  /**
   * Remove a digital object from a GAMS collection
   * @param collectionId the ID of the GAMS collection
   * @param digitalObjectId the ID of the digital object to remove
   */
  void removeDigitalObjectFromCollection(String collectionId, String digitalObjectId);

  PagedResponse<DigitalObjectCollectionCompactView> findByDigitalObject(String digitalObjectId, Pageable pageable);
  PagedResponse<DigitalObjectListItemView> findDigitalObjectsByCollectionId(String collectionId, Pageable pageable);

  /**
   * Update metadata of a GAMS collection. (Ignores the ID / project abbr and only updates)
   * @param digitalObjectCollection the GAMS collection to update
   */
  void updateMetadata(DigitalObjectCollection digitalObjectCollection);

  /**
   * Delete a GAMS collection by ID
   * @param id the ID of the GAMS collection to delete
   */
  void deleteById(String id);

}
