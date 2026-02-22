package org.ddh.gamsapi.domain.Datastream.utils.interfaces;

import org.ddh.gamsapi.domain.Datastream.utils.dto.DatastreamCreateDto;
import org.ddh.gamsapi.domain.Datastream.utils.dto.DatastreamUpdateDto;
import org.ddh.gamsapi.domain.Datastream.utils.exceptions.*;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import org.ddh.gamsapi.domain.Datastream.Datastream;
import org.ddh.gamsapi.domain.Datastream.DatastreamId;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNoMainResourceDatastreamDefinedException;
import org.ddh.gamsapi.infrastructure.System.dto.PagedResponse;

import java.util.List;
import java.util.Set;

public interface IDatastreamService {
  void delete(Datastream datastream);

  Datastream findById(DatastreamId id) throws DatastreamNotFoundException;


  Datastream save(Datastream datastream, MultipartFile file);


  List<IDatastreamDetailsView> findAll(DigitalObject digitalObject);

  /**
   * Allows to findall datastreams for a given digital object id combined with pagination
   * @param digitalObjectId digital object id
   * @param pageable pagination information
   * @return page of datastream projections
   */
  PagedResponse<IDatastreamDetailsView> findAll(String digitalObjectId, Pageable pageable);

  IDatastreamDetailsView findDatastreamDetailsById(DatastreamId dsid) throws DatastreamNotFoundException;

  /**
   * Finds and returns a singular datastream projection by the given digital object id and datastream id.
   * Throws DatastreamNotFoundException if no datastream is found.
   * @param digitalObjectId datastream's parent digital object id
   * @param tags tags to be used for searching
   * @throws DatastreamNotFoundException when no datastreams were found
   * @throws DatastreamAmbiguousMatchException when multiple datastreams were found
   * @throws DigitalObjectNotFoundException when the digital object does not exist
   * @return found datastream projection
   */
  IDatastreamDetailsView findSingularDatastreamDetailsViewByObjectIdAndTags(String digitalObjectId, Set<String> tags) throws DatastreamNotFoundException, DigitalObjectNotFoundException;


  /**
   * Finds and returns a singular datastream projection ("the main datastream") by the given digital object id.
   * The main datastream is defined by the digital object itself via mainResource property.
   * @param digitalObjectId digital object that defines it's main datastream
   * @throws DigitalObjectNotFoundException (unchecked) if the digital object does not exist
   * @throws DatastreamNotFoundException (unchecked) if the main datastream does not exist
   * @throws DigitalObjectNoMainResourceDatastreamDefinedException (unchecked) if the digital object has no mainResource datastream defined.
   * @return found mainResource datastream projection
   */
  IDatastreamDetailsView findMainDatastreamByDigitalObjectId(String digitalObjectId);


  /**
   * Finds and returns a page of datastream projections by the given digital object id and tags.
   * The tags are used to filter the datastreams.
   * @param digitalObjectId digital object id
   * @param tags tags to be used for searching
   * @param pageable pagination information
   * @return page of datastream projections
   */
  PagedResponse<IDatastreamDetailsView> findAll(String digitalObjectId, Set<String> tags, Pageable pageable);


  /**
   * Finds all datastream ids for a given digital object id.
   * @param digitalObjectId the id of the digital object
   * @param pageable pagination information
   * @return a page of datastream ids
   * @throws DigitalObjectNotFoundException if the digital object does not exist
   */
  PagedResponse<String> findAllIds(String digitalObjectId, Pageable pageable) throws DigitalObjectNotFoundException;

  /**
   * Creates a new datastream from a direct file upload (bypassing BagIt workflow).
   * The dsid is provided explicitly by the caller.
   * Checksums (MD5 + SHA-512) are computed server-side during file write.
   *
   * @param digitalObjectId the parent digital object ID
   * @param dsid the datastream identifier (from path variable)
   * @param dto metadata provided by the user
   * @param file the uploaded file
   * @return the created datastream
   * @throws DigitalObjectNotFoundException if the digital object doesn't exist
   * @throws DatastreamAlreadyExistsException if a datastream with this dsid already exists
   * @throws DatastreamCannotWriteFileException if file write fails
   */
  Datastream createFromUpload(String digitalObjectId, String dsid, DatastreamCreateDto dto,
                              MultipartFile file);

  // --- Add these methods to IDatastreamService interface ---

  /**
   * Updates metadata fields of an existing datastream.
   * Only non-null fields from the patch DTO are applied.
   * The dsid and digitalObject (composite PK) cannot be changed.
   *
   * @param digitalObjectId the parent digital object ID
   * @param dsid the datastream identifier
   * @param patch DTO containing the fields to update
   * @return the updated datastream details projection
   * @throws DatastreamNotFoundException if the datastream does not exist
   * @throws DigitalObjectNotFoundException if the digital object does not exist
   * @throws DatastreamValidationException if the patch would violate invariants
   */
  IDatastreamDetailsView updateDatastream(String digitalObjectId, String dsid,
                                          DatastreamUpdateDto patch);

  /**
   * Updates the content (file) of an existing datastream.
   * Recomputes checksums and updates file size and MIME type.
   * The dsid and digitalObject reference remain unchanged.
   *
   * @param digitalObjectId the parent digital object ID
   * @param dsid the datastream identifier
   * @param file the new file content
   * @return the updated datastream details projection
   * @throws DatastreamNotFoundException if the datastream does not exist
   * @throws DigitalObjectNotFoundException if the digital object does not exist
   * @throws DatastreamValidationException if the file is empty
   * @throws DatastreamCannotWriteFileException if file write fails
   */
  IDatastreamDetailsView updateDatastreamContent(String digitalObjectId, String dsid,
                                                 MultipartFile file);
}
