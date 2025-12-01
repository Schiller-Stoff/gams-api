package org.ddh.gamsapi.domain.DigitalObject.utils.interfaces;

import org.springframework.data.domain.Pageable;
import org.springframework.util.MultiValueMap;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.utils.dto.DigitalObjectCompactDTO;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObjectDublinCoreSpecification;
import org.ddh.gamsapi.domain.DigitalObject.utils.dto.DigitalObjectSearchResultDTO;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.infrastructure.System.dto.PagedResponse;

import java.util.Optional;
import java.util.Set;

public interface IDigitalObjectService {

  DigitalObject save(DigitalObject digitalObject);


  /**
   * Find all digital objects for a given project (with project abbreviation). Substring filter for digital object's id.
   * @param projectAbbr identifier of the project
   * @param idSearchTerm startswith filter for digital object's id
   * @param pageable pagination
   * @return a page of digital objects as projection
   */
  PagedResponse<DigitalObjectListItemView> findAllByProjectAbbr(String projectAbbr, String idSearchTerm, Pageable pageable);

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
   * Find all digital objects for a given project (with project abbreviation) and just return their ids.
   * @param projectAbbr identifier of the project
   * @param pageable pagination
   * @return a list of digital object ids
   */
  PagedResponse<String> findAllIdsByProjectAbbr(String projectAbbr, Pageable pageable);


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


  /**
   * Find all digital objects for a given project and set of tags.
   * @param projectAbbr project the digital objects belong to
   * @param tags set of tags the digital objects must have
   * @param pageable pagination information
   * @return page of digital objects matching the criteria
   */
  PagedResponse<DigitalObjectListItemView> findAllByProjectAndTags(
      String projectAbbr,
      Set<String> tags,
      Pageable pageable
  );


  /**
   * Find distinct tags for a given project.
   * @param projectAbbr project abbreviation
   * @return set of distinct tags
   */
  Set<String> findDistinctTagsByProject(String projectAbbr);

}
