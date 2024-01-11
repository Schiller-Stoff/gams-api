package org.zim.gamsapi.Datastream.interfaces;

import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.exceptions.DatastreamNotFoundException;
import org.zim.gamsapi.DigitalObject.DigitalObject;

import java.util.List;

public interface IDatastreamService {
  void delete(Datastream datastream);

  /**
   * Deletes a datastream defined by it's parent digital object and it's user specified datastream-id.
   * @param digitalObject Parent digital object
   * @param dsid datastream-id like TEI_SOURCE
   */
  void delete(DigitalObject digitalObject, String dsid);

  Datastream findById(Long id) throws DatastreamNotFoundException;

  /**
   * Returns a datastream based on the parent digital object, and it's datastream-identifier.
   * @param pid of the parent digital object
   * @param dsid user defined datastream-identifier (must be unique per object)
   * @return found Datastream
   */
  Datastream findByDsid(String pid, String dsid) throws DatastreamNotFoundException;

  Datastream save(Datastream datastream);


  List<IDatastreamDetailsView> findAll(DigitalObject digitalObject);

  IDatastreamDetailsView findDatastreamDetailsByDsid(String objectId, String dsid) throws DatastreamNotFoundException;

}
