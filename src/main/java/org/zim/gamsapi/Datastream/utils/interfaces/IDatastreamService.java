package org.zim.gamsapi.Datastream.utils.interfaces;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.DatastreamId;
import org.zim.gamsapi.Datastream.utils.exceptions.DatastreamAmbiguousMatchException;
import org.zim.gamsapi.Datastream.utils.exceptions.DatastreamNotFoundException;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.DigitalObject.utils.exceptions.DigitalObjectNoMainResourceDatastreamDefinedException;
import org.zim.gamsapi.System.dto.PagedResponse;

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

}
