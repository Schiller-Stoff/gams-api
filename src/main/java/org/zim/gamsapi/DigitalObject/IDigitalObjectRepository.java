package org.zim.gamsapi.DigitalObject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectDetailsView;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectIdView;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface IDigitalObjectRepository extends CrudRepository<DigitalObject, String> {

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
   * Find all digital objects for a given project (with project abbreviation) and filter for objectType and types.
   * @param projectAbbr identifier of the project
   * @param objectType filter by object type
   * @param types filter by types
   * @param pageable pagination
   * @return a page of digital objects as projection
   */
  Page<DigitalObjectListItemView> findDigitalObjectsByProject_ProjectAbbrAndObjectTypeAndTypesIn(String projectAbbr, String objectType, Set<String> types, Pageable pageable);

  /**
   * Find all digital objects for a given project (with project abbreviation) and filter for objectType.
   * @param projectAbbr identifier of the project
   * @param objectType filter by object type
   * @param pageable pagination
   * @return a page of digital objects as projection
   */
  Page<DigitalObjectListItemView> findDigitalObjectsByProject_ProjectAbbrAndObjectType(String projectAbbr, String objectType, Pageable pageable);

  /**
   * Find all digital objects for a given project (with project abbreviation) and filter for types.
   * @param projectAbbr identifier of the project
   * @param types filter by types
   * @param pageable pagination
   * @return a page of digital objects as projection
   */
  Page<DigitalObjectListItemView> findDigitalObjectsByProject_ProjectAbbrAndTypesIn(String projectAbbr, Set<String> types, Pageable pageable);


  /**
   * Find a digital object by its id.
   * @param id the id of the digital object
   * @return a digital object as projection
   */
  Optional<DigitalObjectDetailsView> findDigitalObjectById(String id);

  /**
   * Find all digital objects for a given project (via project abbreviation) as projection.
   * @param projectAbbr identifier of the project
   * @return a list of digital object ids
   */
  List<DigitalObjectIdView> findAllByProject_ProjectAbbr(String projectAbbr);

}
