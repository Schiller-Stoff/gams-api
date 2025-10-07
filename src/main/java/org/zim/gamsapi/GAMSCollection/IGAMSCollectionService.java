package org.zim.gamsapi.GAMSCollection;

import org.springframework.data.domain.Pageable;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.GAMSCollection.interfaces.GAMSCollectionDetailsView;
import org.zim.gamsapi.GAMSCollection.interfaces.GamsCollectionCompactView;
import org.zim.gamsapi.System.dto.PagedResponse;

import java.util.List;

public interface IGAMSCollectionService {
  void save(GAMSCollection GAMSCollection);
  void delete(GAMSCollection GAMSCollection);
  GAMSCollectionDetailsView findById(String id);
  PagedResponse<GamsCollectionCompactView> findAll(Pageable pageable);
  List<GAMSCollection> findByProjectAbbr(String projectAbbr);
  PagedResponse<GamsCollectionCompactView> findByProjectAbbr(String projectAbbr, Pageable pageable);
  void addDigitalObjectToCollection(String collectionId, String digitalObjectId);

  /**
   * Remove a digital object from a GAMS collection
   * @param collectionId the ID of the GAMS collection
   * @param digitalObjectId the ID of the digital object to remove
   */
  void removeDigitalObjectFromCollection(String collectionId, String digitalObjectId);

  PagedResponse<GamsCollectionCompactView> findByDigitalObject(String digitalObjectId, Pageable pageable);
  PagedResponse<DigitalObjectListItemView> findDigitalObjectsByCollectionId(String collectionId, Pageable pageable);

  /**
   * Update metadata of a GAMS collection. (Ignores the ID / project abbr and only updates)
   * @param gamsCollection the GAMS collection to update
   */
  void updateMetadata(GAMSCollection gamsCollection);

  /**
   * Delete a GAMS collection by ID
   * @param id the ID of the GAMS collection to delete
   */
  void deleteById(String id);

}
