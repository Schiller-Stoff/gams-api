package org.zim.gamsapi.DigitalObject.DublinCoreEntry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import java.util.List;

/**
 * Repository for the DublinCoreElement entity.
 */
public interface IDublinCoreEntryRepository extends JpaRepository<DublinCoreEntry, Long> {

  //TODO jdoc
  List<DublinCoreEntry> findByDigitalObjectAndName(DigitalObject digitalObject, String name);

  //TODO jdoc
  List<DublinCoreEntry> findByDigitalObject(DigitalObject digitalObject);

  // TODO jdoc
  @Query(value = "SELECT dcm FROM DublinCoreEntry dcm WHERE dcm.digitalObject.id = :digitalObjectId " +
      "AND dcm.name = :name")
  List<DublinCoreEntry> findMetadataByDigitalObjectIdAndName(String digitalObjectId, String name);

  // Efficiently find objects by metadata value
  // + ignore case
  @Query(value = "SELECT DISTINCT dcm.digitalObject FROM DublinCoreEntry dcm " +
      "WHERE dcm.name = :name AND LOWER(dcm.value) LIKE CONCAT('%', LOWER(:value), '%')")
  List<DigitalObject> findDigitalObjectsByDublinCoreElementValue(String name, String value);



}
