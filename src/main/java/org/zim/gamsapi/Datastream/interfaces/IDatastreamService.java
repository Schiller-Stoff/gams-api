package org.zim.gamsapi.Datastream.interfaces;

import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.DatastreamId;
import org.zim.gamsapi.Datastream.exceptions.DatastreamNotFoundException;
import org.zim.gamsapi.DigitalObject.DigitalObject;

import java.util.List;

public interface IDatastreamService {
  void delete(Datastream datastream);

  Datastream findById(DatastreamId id) throws DatastreamNotFoundException;


  Datastream save(Datastream datastream);


  List<IDatastreamDetailsView> findAll(DigitalObject digitalObject);

  IDatastreamDetailsView findDatastreamDetailsByDsid(String objectId, DatastreamId dsid) throws DatastreamNotFoundException;

}
