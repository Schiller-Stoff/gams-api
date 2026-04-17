package org.ddh.gamsapi.domain.DigitalObject.utils.interfaces;

import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.utils.dto.DigitalObjectCompactDTO;
import org.ddh.gamsapi.domain.DigitalObject.utils.dto.DigitalObjectCreateDto;
import org.ddh.gamsapi.domain.DigitalObject.utils.dto.DigitalObjectUpdateDto;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectValidationException;
import org.ddh.gamsapi.infrastructure.System.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

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

  /**
   * Allows to create a digital object
   * @param projectAbbr project abbreviation
   * @param dto command object to create a digital object
   * @return created digital object
   */
  DigitalObject create(String projectAbbr, DigitalObjectCreateDto dto);

  /**
   * Selectively updates a digital object's metadata.
   * Only non-null fields from the patch DTO are applied.
   *
   * @param id the digital object ID
   * @param patch DTO containing only the fields to update
   * @return the updated digital object as compact DTO
   * @throws DigitalObjectNotFoundException if the object doesn't exist
   * @throws DigitalObjectValidationException if the merge would violate invariants
   */
  DigitalObjectCompactDTO updateDigitalObject(String id, DigitalObjectUpdateDto patch);
}
