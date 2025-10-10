package org.zim.gamsapi.domain.DigitalObject.utils.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.zim.gamsapi.domain.DigitalObject.DigitalObject;

import java.util.Date;
import java.util.List;
import java.util.Optional;

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
  Optional<Date> findMaxLastModifiedDateByProjectAbbr(@Param("projectAbbr") String projectAbbr);

  /**
   * Finds all digital objects for a given collection.
   * @param collectionId identifier of the collection
   * @param pageable pagination
   * @return a page of digital objects as projection
   */
  @Query("SELECT do FROM GAMSCollection c JOIN c.digitalObjects do WHERE c.id = :collectionId")
  Page<DigitalObjectListItemView> findDigitalObjectsByCollectionId(
      @Param("collectionId") String collectionId,
      Pageable pageable
  );



}
