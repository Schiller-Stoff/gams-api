package org.zim.gamsapi.DigitalObject.DublinCoreEntry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import java.util.List;

/**
 * Repository for the DublinCoreElement entity.
 */
public interface IDublinCoreEntryRepository extends JpaRepository<DublinCoreEntry, Long> {

  /**
   * Find DublinCoreEntries by digital object and name.
   * @param digitalObject digital object
   * @param name name of the DublinCoreElement
   * @return a list of DublinCoreEntries
   */
  List<DublinCoreEntry> findByDigitalObjectAndName(DigitalObject digitalObject, String name);

  /**
   * Find DublinCoreEntries by digital object.
   * @param digitalObject digital object
   * @return a list of DublinCoreEntries
   */
  List<DublinCoreEntry> findByDigitalObject(DigitalObject digitalObject);

  /**
   * Find DublinCoreEntries by digital object id.
   * @param digitalObjectId digital object id
   * @param name name of the DublinCoreElement
   * @return a list of DublinCoreEntries
   */
  @Query(value = "SELECT dcm FROM DublinCoreEntry dcm WHERE dcm.digitalObject.id = :digitalObjectId " +
      "AND dcm.name = :name")
  List<DublinCoreEntry> findMetadataByDigitalObjectIdAndName(String digitalObjectId, String name);

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
