package org.ddh.gamsapi.domain.DigitalObject.utils.interfaces;

import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface IDigitalObjectRepository extends CrudRepository<DigitalObject, String>, JpaSpecificationExecutor<DigitalObject> {


  @Modifying(flushAutomatically = true)
  void deleteAllByProject_ProjectAbbr(String projectAbbr);


  /**
   * Deletes all digital objects for a given project (with project abbreviation).
   * @param projectAbbr identifier of the project to be deleted
   */
  @Query("DELETE FROM DigitalObject d WHERE d.project.projectAbbr = :projectAbbr")
  @Modifying(flushAutomatically = true)
  void deleteAll(@Param("projectAbbr") String projectAbbr);

  /**
   * Find all digital objects for a given project (with project abbreviation).
   * @param projectAbbr identifier of the project
   * @return a list of digital objects
   */
  List<DigitalObject> findDigitalObjectsByProject_ProjectAbbr(String projectAbbr);

  /**
   * Find all digital objects for a given project (with project abbreviation).
   * @param projectAbbr identifier of the project
   * @param pageable pagination
   * @return a page of digital objects as projection
   */
  Page<DigitalObjectListItemView> findDigitalObjectsByProject_ProjectAbbr(String projectAbbr, Pageable pageable);

  /**
   * Find all digital objects for a given project (with project abbreviation) and substring filter of digital object's id.
   * @param projectAbbr identifier of the project
   * @param id substring filter for digital object's id
   * @param pageable pagination
   * @return a page of digital objects as projection
   */
  Page<DigitalObjectListItemView> findDigitalObjectsByProject_ProjectAbbrAndIdIsContainingIgnoreCase(String projectAbbr, String id, Pageable pageable);

  /**
   * Find all digital objects for a given project (with project abbreviation) and filter for objectType.
   * @param projectAbbr identifier of the project
   * @param objectType filter by object type
   * @param pageable pagination
   * @return a page of digital objects as projection
   */
  Page<DigitalObjectListItemView> findDigitalObjectsByProject_ProjectAbbrAndObjectType(String projectAbbr, String objectType, Pageable pageable);

  /**
   * Find a digital object by its id.
   * @param id the id of the digital object
   * @return a digital object as projection
   */
  Optional<DigitalObjectDetailsView> findDigitalObjectById(String id);

  /**
   * Find all digital objects for a given project (via project abbreviation) as projection.
   * @param projectAbbr identifier of the project
   * @param pageable pagination
   * @return a list of digital object ids
   */
  Page<DigitalObjectIdView> findAllByProject_ProjectAbbr(String projectAbbr, Pageable pageable);

  /**
   * Find all digital objects for a given project (via project abbreviation) as projection.
   * @param projectAbbr identifier of the project
   * @return a list of digital object ids
   */
  List<DigitalObjectIdView> findAllByProject_ProjectAbbr(String projectAbbr);

  /**
   * Finds the most recent modification timestamp for any digital object for given project.
   * E.g. for project 'memo' it returns a date for the latest modified digital object.
   * @param projectAbbr The abbreviation of the project.
   * @return The last modified date of the digital object.
   */
  @Query("SELECT MAX(do.modified) FROM DigitalObject do WHERE do.project.projectAbbr = :projectAbbr")
  Optional<Instant> findMaxLastModifiedDateByProjectAbbr(@Param("projectAbbr") String projectAbbr);

  /**
   * Finds digital objects by project and tags using AND logic.
   * All specified tags must be present on the digital object.
   *
   * @param projectAbbr Project abbreviation
   * @param tags Set of tags (all must match)
   * @param tagCount Number of tags (must equal tags.size() for AND logic)
   * @param pageable Pagination
   * @return Page of digital objects matching ALL tags
   */
  @Query("SELECT d FROM DigitalObject d " +
      "WHERE d.project.projectAbbr = :projectAbbr " +
      "AND (SELECT COUNT(DISTINCT t) FROM d.tags t WHERE t IN :tags) = :tagCount")
  Page<DigitalObjectListItemView> findByProject_ProjectAbbrAndTagsIn(
      @Param("projectAbbr") String projectAbbr,
      @Param("tags") Set<String> tags,
      @Param("tagCount") long tagCount,
      Pageable pageable
  );


  /**
   * Finds all distinct tags used by digital objects in a project.
   * PERFORMANCE: Uses index on (digital_object_id, datastream_tags) and GROUP BY optimization.
   *
   * @param projectAbbr Project abbreviation
   * @return Set of unique tag strings used in the project
   */
  @Query(value =
      "SELECT DISTINCT dt.digital_object_tags " +
          "FROM digital_object_tags dt " +
          "INNER JOIN digital_object dobj ON dt.digital_object_id = dobj.id " +
          "WHERE dobj.project_project_abbr = :projectAbbr " +
          "ORDER BY dt.digital_object_tags ASC",
      nativeQuery = true)
  Set<String> findDistinctTagsByProjectAbbr(@Param("projectAbbr") String projectAbbr);


  /**
   * Find digital objects by project where ID starts with the given prefix.
   * PERFORMANCE: Uses primary key index efficiently. O(log n) complexity.
   * Case-sensitive for maximum performance.
   *
   * @param projectAbbr Project abbreviation
   * @param idPrefix Prefix to match (e.g., "mhdbdb.manuscript")
   * @param pageable Pagination
   * @return Page of matching digital objects
   */
  Page<DigitalObjectListItemView> findDigitalObjectsByProject_ProjectAbbrAndIdStartingWith(
      String projectAbbr,
      String idPrefix,
      Pageable pageable
  );


  /**
   * Checks if any digital objects exist for the given project.
   * Used by ProjectService to provide a clear error message before delete.
   * PERFORMANCE: Spring Data JPA translates this to SELECT EXISTS(...) with
   * an indexed lookup on project_project_abbr. O(1) regardless of object count.
   *
   * @param projectAbbr the project abbreviation
   * @return true if at least one digital object belongs to this project
   */
  boolean existsByProject_ProjectAbbr(String projectAbbr);

}
