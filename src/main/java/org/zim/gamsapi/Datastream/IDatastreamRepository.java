package org.zim.gamsapi.Datastream;

import org.springframework.data.repository.CrudRepository;
import org.zim.gamsapi.DigitalObject.DigitalObject;

import java.util.List;
import java.util.Optional;

public interface IDatastreamRepository extends CrudRepository<Datastream, Long> {

  /**
   * Searches a datastream based on the parent digital object and it's datastream-identifier.
   * @param digitalObject Digital object to be found
   * @param dsid Datastream identifier
   * @return found Datastream
   */
  Optional<Datastream> findByDigitalObjectAndDsid(DigitalObject digitalObject, String dsid);

  /**
   * Deletes a datastream defined by it's parent digital object and it's user specified datastream-id.
   * @param digitalObject Parent digital object
   * @param dsid datastream-id like TEI_SOURCE
   */
  void deleteByDigitalObjectAndDsid(DigitalObject digitalObject, String dsid);
}
