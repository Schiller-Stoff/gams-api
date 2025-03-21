package org.zim.gamsapi.DigitalObject.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
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
   * @param pageable pagination
   * @return a page of digital objects as projection
   */
  Page<DigitalObjectListItemView> findAllByProjectAbbr(String projectAbbr, Optional<String> objectType, Pageable pageable);

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

  /**
   * Find all digital objects for a given project (with project abbreviation) and Dublin Core Element name and value.
   * @param projectAbbr identifier of the project
   * @param dcEntryName name of the Dublin Core Element
   * @param dcEntryValue value of the Dublin Core Element
   * @param pageAble pagination
   * @return
   */
  Page<DigitalObjectListItemView> findDigitalObjectsByProjectAbbrAndDublinCore(String projectAbbr, String dcEntryName, String dcEntryValue, Pageable pageAble);
}
