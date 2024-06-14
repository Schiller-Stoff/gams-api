package org.zim.gamsapi.DigitalObject.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.Project.Project;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface IDigitalObjectService {

  DigitalObject save(DigitalObject digitalObject);

  /**
   * Find all digital objects.
   * @return list of digital objects
   */
  List<DigitalObject> findAll();

  /**
   * Find all digital objects for a given project (with project abbreviation).
   * @param projectAbbr identifier of the project
   * @param pageable pagination
   * @return a page of digital objects as projection
   */
  Page<DigitalObjectListItemView> findAllByProjectAbbr(String projectAbbr, Pageable pageable);

  /**
   * Find all digital objects for a given project (with project abbreviation). Substring filter for digital object's id.
   * @param projectAbbr identifier of the project
   * @param containedInPid substring filter for digital object's id
   * @param pageable pagination
   * @return a page of digital objects as projection
   */
  Page<DigitalObjectListItemView> findAllByProjectAbbr(String projectAbbr, String containedInPid, Pageable pageable);

  /**
   * Find all digital objects for a given project (with project abbreviation). Filter by object type.
   * @param projectAbbr identifier of the project
   * @param objectType filter by object type (optionally)
   * @param types filter by types (optionally)
   * @param pageable pagination
   * @return a page of digital objects as projection
   */
  Page<DigitalObjectListItemView> findAllByProjectAbbr(String projectAbbr, Optional<String> objectType, Optional<Set<String>> types, Pageable pageable);

  /**
   * Find all digital objects for a given project (with project abbreviation).
   * @param projectAbbr identifier of the project
   * @return a list of digital objects
   */
  List<DigitalObject> findAllByProjectAbbr(String projectAbbr);

  /**
   * Find a digital object by its id.
   * @param pid the id of the digital object
   * @return the digital object
   * @throws DigitalObjectNotFoundException if the digital object cannot be found
   */
  DigitalObject findById(String pid) throws DigitalObjectNotFoundException;

  void delete(DigitalObject digitalObject);

  void deleteAllForProject(Project project);

  /**
   * Allows to assign child objects to a parent object.
   * @param digitalObject object to which the parent should be assigned to
   * @param parent the parent object
   * @return the parent object with the assigned child objects
   */
  DigitalObject assignParentObject(DigitalObject digitalObject, DigitalObject parent);


  /**
   * Find a digital object by its id.
   * @param id the id of the digital object
   * @return a digital object as projection
   */
  DigitalObjectDetailsView findDigitalObjectDetailsViewById(String id);


  /**
   * Find all digital objects for a given project (with project abbreviation) and just return their ids.
   * @param projectAbbr identifier of the project
   * @return a list of digital object ids
   */
  List<String> findAllIdsByProjectAbbr(String projectAbbr);

}
