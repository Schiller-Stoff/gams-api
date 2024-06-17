package org.zim.gamsapi.Datastream;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamDetailsView;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamIdView;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import java.util.List;
import java.util.Optional;

public interface IDatastreamRepository extends CrudRepository<Datastream, DatastreamId> {

  /**
   * Deletes all datastreams for a given project (with project abbreviation).
   * @param projectAbbr identifier of the project to be deleted
   */
  @Query(nativeQuery = true, value = "DELETE FROM datastream\n" +
          "WHERE digital_object_id IN (\n" +
          "    SELECT dig_obj.id\n" +
          "    FROM digital_object dig_obj\n" +
          "             JOIN project cur_proj ON cur_proj.project_abbr = dig_obj.project_project_abbr\n" +
          "    WHERE cur_proj.project_abbr = :projectAbbr\n" +
          ")")
  @Modifying(flushAutomatically = true)
  void deleteAll(String projectAbbr);


  /**
   * Projection method to return a list of datastream details views. Excludes the actual datastream content.
   * Searches a datastream based on the parent digital object and it's datastream-identifier.
   * @param digitalObjectId Digital object to be found
   * @return list of datastream projections.
   */
  List<IDatastreamDetailsView> findAllByDigitalObjectId(String digitalObjectId);

  /**
   * Projection method to return a datastream details views. Excludes the actual datastream content.
   * @param digitalObjectId Digital object to be found
   * @param dsid datastream identifier
   * @return datastream projection.
   */
  Optional<IDatastreamDetailsView> findDatastreamDetailsViewByDigitalObject_IdAndDsid(String digitalObjectId, String dsid);


  void deleteAllByDigitalObject(DigitalObject digitalObject);

  /**
   * Find all datastreams by digital object id and dsid.
   * @param digitalObject digital object
   * @return list of datastream ids
   */
  List<IDatastreamIdView> findAllDatastreamIdViewsByDigitalObject(DigitalObject digitalObject);

}
