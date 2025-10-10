package org.zim.gamsapi.domain.Datastream.utils.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.zim.gamsapi.domain.Datastream.Datastream;
import org.zim.gamsapi.domain.Datastream.DatastreamId;
import org.zim.gamsapi.domain.DigitalObject.DigitalObject;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
   * Projection method to return a page of datastream details views. Excludes the actual datastream content.
   * Searches a datastream based on the parent digital object and it's datastream-identifier.
   * @param digitalObjectId Digital object to be found
   * @param pageable pagination information
   * @return page of datastream projections.
   */
  Page<IDatastreamDetailsView> findAllByDigitalObjectId(String digitalObjectId, Pageable pageable);

  Set<Datastream> findAllByDigitalObject(DigitalObject digitalObject);

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

  /**
   * Find all datastreams by digital object id.
   * @param digitalObjectId digital object id
   * @return list of datastream ids
   */
  Page<IDatastreamIdView> findAllDatastreamIdViewsByDigitalObjectId(String digitalObjectId, Pageable pageable);

  /**
   * Find all datastreams by digital object
   * @param digitalObject digital object
   * @return view of datastreams containing dsid and mimetype
   */
  List<IDatastreamMimeView> findAllDatastreamMimeViewsByDigitalObject(DigitalObject digitalObject);


  /**
   * Finds a list of datastream projections by digital object id and given tags.
   * Given tags are understood via AND logic.
   * @param digitalObjectId id of the datastreams parent digital object
   * @param tags tags to be matched
   * @param tagCount helper argument
   * @param pageable pagination information
   * @return page of datastream projections
   */
  @Query("SELECT d FROM Datastream d WHERE d.digitalObject.id = :digitalObjectId AND " +
      "(SELECT COUNT(DISTINCT t) FROM d.tags t WHERE t IN :tags) = :tagCount")
  Page<IDatastreamDetailsView> findDatastreamsPaginatedByDigitalObject_IdAndTagsIn(
      @Param("digitalObjectId") String digitalObjectId,
      @Param("tags") Set<String> tags,
      @Param("tagCount") long tagCount,
      Pageable pageable
  );

  /**
   * Finds a datastreamDetailsView by digital object id and dsid.
   * @param digitalObjectId Digital object to be found
   * @param dsid datastream identifier
   * @return datastream projection.
   */
  Optional<IDatastreamDetailsView> findDatastreamByDigitalObject_IdAndDsid(String digitalObjectId, String dsid);


  /**
   * Returns the latest modified date of a datastream for given project abbreviation.
   * E.g. for project 'memo' returns the data XYZ because the TEI file from object memo.1 was modified last.
   * @param projectAbbr project abbreviation
   * @return latest modified date of a datastream
   */
  @Query("SELECT MAX(ds.modified) FROM Datastream ds JOIN ds.digitalObject do WHERE do.project.projectAbbr = :projectAbbr")
  Optional<Date> findMaxLastModifiedDateByProjectAbbr(@Param("projectAbbr") String projectAbbr);

  /**
   * Returns the latest modified date of a datastream for given digital object id.
   * @param digitalObjectId digital object id
   * @return latest modified date of a datastream
   */
  @Query("SELECT MAX(ds.modified) FROM Datastream ds JOIN ds.digitalObject do WHERE do.id = :digitalObjectId")
  Optional<Date> findMaxLastModifiedDateByDigitalObjectId(String digitalObjectId);


  @Query("SELECT ds FROM Datastream ds " +
      "WHERE ds.digitalObject.id IN :digitalObjectIds " +
      "AND ds.dsid IN (SELECT do.mainResource FROM DigitalObject do WHERE do.id = ds.digitalObject.id)")
  List<IDatastreamMainResourceView> findMainDatastreamsByDigitalObjectIds(
      @Param("digitalObjectIds") Set<String> digitalObjectIds
  );

}
