package org.zim.gamsapi.DigitalObject.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.Project.Project;

import java.util.List;
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
   * @param parentObject the parent object
   * @param childObjects the child objects
   * @return the parent object with the assigned child objects
   */
  DigitalObject assignChildObjects(DigitalObject parentObject, Set<DigitalObject> childObjects);

}
