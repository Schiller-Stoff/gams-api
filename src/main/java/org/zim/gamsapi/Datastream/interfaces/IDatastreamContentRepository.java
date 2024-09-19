package org.zim.gamsapi.Datastream.interfaces;

import org.springframework.core.io.InputStreamResource;
import org.zim.gamsapi.Datastream.DatastreamId;
import org.zim.gamsapi.Datastream.exceptions.DatastreamCannotDeleteFileException;
import java.nio.file.Path;

/**
 * Handles storing files on the filesystem.
 */
public interface IDatastreamContentRepository {

  DatastreamId save(byte[] data, DatastreamId datastreamId);

  /**
   * Loads the datastream content.
   * @param datastreamId the datastream id to load
   * @return the datastream content
   */
  InputStreamResource findById(DatastreamId datastreamId);

  void delete(DatastreamId datastreamId) throws DatastreamCannotDeleteFileException;

  /**
   * Check if the file exists.
   * @param datastreamId the datastream id to check
   */
  boolean exists(DatastreamId datastreamId);

  /**
   * Calculates the balanced absolute filepath for the given filename for GAMS.
   * The filename is hashed and then balanced to a folder hierarchy.
   * @param datastreamId datastreamId to calculate the balanced filepath for
   * @return the balanced filepath
   */
  Path calcBalancedFilepath(DatastreamId datastreamId);

  /**
   * Deletes all datastream content.
   */
  void deleteAll();

}
