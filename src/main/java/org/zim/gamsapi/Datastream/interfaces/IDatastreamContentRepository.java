package org.zim.gamsapi.Datastream.interfaces;

import org.springframework.core.io.FileSystemResource;
import org.zim.gamsapi.Datastream.exceptions.DatastreamCannotDeleteFileException;

import java.nio.file.Path;

/**
 * Handles storing files on the filesystem.
 */
public interface IDatastreamContentRepository {

  Path save(byte[] data, String fileName);

  FileSystemResource load(String fileName);

  void delete(String fileName) throws DatastreamCannotDeleteFileException;

  /**
   * Check if the file exists.
   * @param fileName the name of the file to check.
   */
  boolean exists(String fileName);

}
