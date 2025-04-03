package org.zim.gamsapi.Datastream.interfaces;

import org.springframework.web.multipart.MultipartFile;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.DatastreamId;
import org.zim.gamsapi.Datastream.exceptions.DatastreamAmbiguousMatchException;
import org.zim.gamsapi.Datastream.exceptions.DatastreamNotFoundException;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;

import java.util.List;
import java.util.Set;

public interface IDatastreamService {
  void delete(Datastream datastream);

  Datastream findById(DatastreamId id) throws DatastreamNotFoundException;


  Datastream save(Datastream datastream, MultipartFile file);


  List<IDatastreamDetailsView> findAll(DigitalObject digitalObject);

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
   * @throws org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNoMainResourceDatastreamDefinedException (unchecked) if the digital object has no mainResource datastream defined.
   * @return found mainResource datastream projection
   */
  IDatastreamDetailsView findMainDatastreamByDigitalObjectId(String digitalObjectId);

}
