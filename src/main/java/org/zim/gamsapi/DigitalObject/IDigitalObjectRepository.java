package org.zim.gamsapi.DigitalObject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;

import java.util.List;

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
   * @return list of digital objects
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

}
