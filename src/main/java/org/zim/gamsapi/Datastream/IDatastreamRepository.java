package org.zim.gamsapi.Datastream;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
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


  //@Query("DELETE FROM Datastream d WHERE d.digitalObject.project.projectAbbr = 'demo'")
  @Query(nativeQuery = true, value = "DELETE FROM datastream\n" +
          "WHERE digital_object_id IN (\n" +
          "    SELECT dig_obj.id\n" +
          "    FROM digital_object dig_obj\n" +
          "             JOIN project cur_proj ON cur_proj.project_abbr = dig_obj.project_project_abbr\n" +
          "    WHERE cur_proj.project_abbr = 'demo'\n" +
          ")")
  @Modifying(flushAutomatically = true)
  void deleteAllCustom(String projectAbbr);
}
