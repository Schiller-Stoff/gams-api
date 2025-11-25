package org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry;

import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

/**
 * Repository for the DublinCoreElement entity.
 */
public interface IDublinCoreEntryRepository extends JpaRepository<DublinCoreEntry, Long>, JpaSpecificationExecutor<DublinCoreEntry> {

  /**
   * Find DublinCoreEntries by digital object and name.
   * @param digitalObject digital object
   * @param name name of the DublinCoreElement
   * @return a list of DublinCoreEntries
   */
  List<DublinCoreEntrySummaryView> findByDigitalObjectAndName(DigitalObject digitalObject, String name);

    /**
     * Find DublinCoreEntries by digital object id.
     * @param digitalObjectId
     * @return
     */
  List<DublinCoreEntry> findEntriesByDigitalObjectId(String digitalObjectId);

  /**
   * Find DublinCoreEntries by digital object.
   * @param digitalObject digital object
   * @return a list of DublinCoreEntries
   */
  List<DublinCoreEntrySummaryView> findByDigitalObject(DigitalObject digitalObject);

  /**
   * Find DublinCoreEntries by digital object id.
   * @param digitalObjectId digital object id
   * @return a list of DublinCoreEntries
   */
  List<DublinCoreEntrySummaryView> findByDigitalObjectId(String digitalObjectId);

  /**
   * Find DublinCoreEntries by digital object ids.
   * @param digitalObjectIds set of digital object ids
   * @return a list of DublinCoreEntries
   */
  List<DublinCoreEntrySummaryView> findByDigitalObjectIdIn(Set<String> digitalObjectIds);

  /**
   * Find DublinCoreEntries by digital object id.
   * @param digitalObjectId digital object id
   * @param name name of the DublinCoreElement
   * @return a list of DublinCoreEntries
   */
  @Query(value = "SELECT dcm FROM DublinCoreEntry dcm WHERE dcm.digitalObject.id = :digitalObjectId " +
      "AND dcm.name = :name")
  List<DublinCoreEntrySummaryView> findMetadataByDigitalObjectIdAndName(String digitalObjectId, String name);

  /**
   * Find DublinCoreEntries by digital object id.
   * @param name name of the DublinCoreElement
   * @param value value of the DublinCoreElement
   * @return a list of digital objects
   */
  @Query(value = "SELECT DISTINCT dcm.digitalObject FROM DublinCoreEntry dcm " +
      "WHERE dcm.name = :name AND LOWER(dcm.value) LIKE CONCAT('%', LOWER(:value), '%')")
  List<DigitalObject> findDigitalObjectsByDublinCoreElementValue(String name, String value);

  /**
   * Deletes all DublinCoreEntries for a given digital object.
   * @param digitalObject digital object
   */
  void deleteAllByDigitalObject(DigitalObject digitalObject);


}
