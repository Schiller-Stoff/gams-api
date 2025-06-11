package org.zim.gamsapi.DigitalObject.DublinCoreEntry;

import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectIdView;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;
import java.util.List;
import java.util.Set;
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
   * @return a list of DublinCoreEntries
   */
  List<DublinCoreEntrySummaryView> findByDigitalObjectId(String digitalObjectId);

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
   * "Tagged search for dublin core": Finds objects based on exact match of defined dublin core field.
   * Returns the object when at least one of given values was found (exactly) in the DublinCoreElement.
   * E.g. dc:subject = 'food'. Allows to search across multiple projects and define the dc:field to search for.
   * @param projectAbbrs list of project abbreviations
   * @param name name of the DublinCoreElement
   * @param values values to check in the DublinCoreElement
   * @param pageable pagination information
   * @return a page of digital objects
   */
  @Query(value = "SELECT do FROM DigitalObject do " +
      "JOIN DublinCoreEntry dcm ON dcm.digitalObject = do " +
      "WHERE dcm.name = :name " +
      "AND dcm.value IN :values " +
      "AND do.project.projectAbbr IN :projectAbbrs " +
      "GROUP BY do.id " +
      "ORDER BY MIN(dcm.value)"
  )
  @QueryHints(value = {
      @QueryHint(name = HINT_FETCH_SIZE, value = "50"),
      @QueryHint(name = HINT_READONLY, value = "true")
  })
  Page<DigitalObjectListItemView> findDigitalObjectListItemViewsByProjectAbbrsAndDublinCoreElementFixedValues(
      Set<String> projectAbbrs, String name, List<String> values, Pageable pageable);


  /**
   * Fulltext search over all dublin core fields of a digital object.
   * Allows to search across multiple projects.
   * @param projectAbbrs list of project abbreviations
   * @param searchTerm search term
   * @param pageable pagination information
   * @return a page of digital objects
   */
  @Query(value = "SELECT DISTINCT do FROM DigitalObject do " +
      "JOIN DublinCoreEntry dcm ON dcm.digitalObject = do " +
      "WHERE LOWER(dcm.value) LIKE CONCAT('%', LOWER(:searchTerm), '%') " +
      "AND do.project.projectAbbr IN :projectAbbrs")
  @QueryHints(value = {
      @QueryHint(name = HINT_FETCH_SIZE, value = "50"),
      @QueryHint(name = HINT_READONLY, value = "true")
  })
  Page<DigitalObjectListItemView> findDigitalObjectsByDCFulltext(
      @Param("projectAbbrs") Set<String> projectAbbrs,
      @Param("searchTerm") String searchTerm,
      Pageable pageable);

  /**
   * Fulltext search over all dublin core fields of a digital object -> allows to retrict search to certain specific dc-fields.
   * @param projectAbbrs list of project abbreviations
   * @param elementNames list of DublinCoreElement names
   * @param searchTerm search term
   * @param pageable pagination information
   * @return a page of digital objects
   */
  @Query(value = "SELECT DISTINCT do FROM DigitalObject do " +
      "JOIN DublinCoreEntry dcm ON dcm.digitalObject = do " +
      "WHERE dcm.name IN :elementNames " +
      "AND LOWER(dcm.value) LIKE CONCAT('%', LOWER(:searchTerm), '%') " +
      "AND do.project.projectAbbr IN :projectAbbrs")
  @QueryHints(value = {
      @QueryHint(name = HINT_FETCH_SIZE, value = "50"),
      @QueryHint(name = HINT_READONLY, value = "true")
  })
  Page<DigitalObjectListItemView> findDigitalObjectsByFulltextOnSpecificElements(
      @Param("projectAbbrs") Set<String> projectAbbrs,
      @Param("elementNames") Set<String> elementNames,
      @Param("searchTerm") String searchTerm,
      Pageable pageable);

}
