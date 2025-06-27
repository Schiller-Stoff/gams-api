package org.zim.gamsapi.DigitalObject.interfaces;

import org.springframework.data.domain.Pageable;
import org.springframework.util.MultiValueMap;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.dto.DigitalObjectCompactDTO;
import org.zim.gamsapi.DigitalObject.DigitalObjectDublinCoreSpecification;
import org.zim.gamsapi.DigitalObject.dto.DigitalObjectSearchResultDTO;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.System.dto.PagedResponse;

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
   * Find all digital objects for a given project (with project abbreviation). Substring filter for digital object's id.
   * @param projectAbbr identifier of the project
   * @param containedInPid substring filter for digital object's id
   * @param pageable pagination
   * @return a page of digital objects as projection
   */
  PagedResponse<DigitalObjectListItemView> findAllByProjectAbbr(String projectAbbr, String containedInPid, Pageable pageable);

  /**
   * Find all digital objects for a given project (with project abbreviation). Filter by object type.
   * @param projectAbbr identifier of the project
   * @param objectType filter by object type (optionally)
   * @param pageable pagination
   * @return a page of digital objects as projection
   */
  PagedResponse<DigitalObjectListItemView> findAllByProjectAbbr(String projectAbbr, Optional<String> objectType, Pageable pageable);


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
   * @param pageable pagination
   * @return a list of digital object ids
   */
  PagedResponse<String> findAllIdsByProjectAbbr(String projectAbbr, Pageable pageable);


  /**
   * Fulltext search over objects of defined project. Searches for string occurrence in defined dublin core fields.
   * The object is being returned when one value in the value list matches exactly.
   * @param projectAbbrs list of project abbreviations
   * @param dcEntries list of DublinCoreElement names. If empty all dublin core fields will be searched
   * @param fulltext fulltext search string
   * @param pageAble pagination
   * @return a page of digital objects
   */
  PagedResponse<DigitalObjectListItemView> searchByDCFulltext(Set<String> projectAbbrs, Set<String> dcEntries, String fulltext, Pageable pageAble);

  /**
   * Find a digital object by its id and return a compact DTO representation.
   * @param id the id of the digital object
   * @return a DigitalObjectCompactDTO representation of the digital object
   */
  DigitalObjectCompactDTO findDigitalObjectCompactDTOById(String id);

  /**
   * Search digital objects by Dublin Core criteria.
   * @param dublinCoreFilters a map of Dublin Core filters where the key is the Dublin Core element name and the value is a list of values to match
   * @param projectAbbrs a set of project abbreviations to filter the digital objects by their associated projects
   * @param searchMode the search mode to use (e.g., exact match, fulltext)
   * @param pageable pagination information
   * @return a page of DigitalObjectSearchResultDTO containing the search results
   */
  PagedResponse<DigitalObjectSearchResultDTO> searchDigitalObjectsByDublinCoreCriteria(
      MultiValueMap<String, String> dublinCoreFilters,
      Set<String> projectAbbrs,
      DigitalObjectDublinCoreSpecification.SearchMode searchMode,
      Pageable pageable);
}
