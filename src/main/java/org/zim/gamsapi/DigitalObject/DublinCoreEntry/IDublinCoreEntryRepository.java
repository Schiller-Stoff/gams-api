package org.zim.gamsapi.DigitalObject.DublinCoreEntry;

import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;
import java.util.List;
import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.QueryHints.HINT_READONLY;

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
  List<DublinCoreEntrySummaryView> findByDigitalObjectAndName(DigitalObject digitalObject, String name);

  /**
   * Find DublinCoreEntries by digital object.
   * @param digitalObject digital object
   * @return a list of DublinCoreEntries
   */
  List<DublinCoreEntrySummaryView> findByDigitalObject(DigitalObject digitalObject);

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

  /**
   * Find digital objects by project abbreviation and DublinCoreElement name and value.
   * See also: https://claude.ai/chat/5a33a7d3-40b4-48a1-aee7-1b010bf1a7e1
   * @param projectAbbr project abbreviation
   * @param name name of the DublinCoreElement
   * @param value value of the DublinCoreElement
   * @param pageable pagination information
   * @return a page of digital objects
   */
  @Query(value = "SELECT do FROM DigitalObject do " +
      "JOIN DublinCoreEntry dcm ON dcm.digitalObject = do " +
      "WHERE dcm.name = :name " +
      "AND LOWER(dcm.value) LIKE CONCAT('%', LOWER(:value), '%') " +
      "AND do.project.projectAbbr = :projectAbbr " +
      "GROUP BY do.id " +
      "ORDER BY MIN(dcm.value)"
  )
  @QueryHints(value = {
      @QueryHint(name = HINT_FETCH_SIZE, value = "50"),
      @QueryHint(name = HINT_READONLY, value = "true")
  })
  Page<DigitalObjectListItemView> findDigitalObjectListItemViewsByProjectAbbrAndDublinCoreElementValue(
      String projectAbbr, String name, String value, Pageable pageable);



   //Option 2: Native query using fulltext search (provided by POSTGRES)
//  @Query(value = "SELECT do.id, do.object_type, do.publisher, do.created, do.published, " +
//      "do.modified, do.created_by, do.modified_by, p.project_abbr, " +
//      "mb.title, mb.description " +
//      "FROM digital_object do " +
//      "JOIN project p ON do.project_project_abbr = p.project_abbr " +
//      "JOIN dublin_core_entry dcm ON dcm.digital_object_id = do.id " +
//      "WHERE dcm.name = :name " +
//      "AND to_tsvector('english', dcm.value) @@ plainto_tsquery('english', :value) " +
//      "AND do.project_project_abbr = :projectAbbr",
//      nativeQuery = true,
//      countQuery = "SELECT COUNT(DISTINCT do.id) FROM digital_object do " +
//          "JOIN dublin_core_entry dcm ON dcm.digital_object_id = do.id " +
//          "WHERE dcm.name = :name " +
//          "AND to_tsvector('english', dcm.value) @@ plainto_tsquery('english', :value) " +
//          "AND do.project_project_abbr = :projectAbbr")
//  Page<Object[]> findDigitalObjectsByProjectAbbrAndDublinCoreElementValueFullText(
//      String projectAbbr, String name, String value, Pageable pageable);

}
