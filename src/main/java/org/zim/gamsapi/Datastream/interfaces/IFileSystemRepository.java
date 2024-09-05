package org.zim.gamsapi.Datastream.interfaces;

import org.springframework.core.io.FileSystemResource;

import java.nio.file.Path;

/**
 * Handles storing files on the filesystem.
 */
public interface IFileSystemRepository {

  Path save(byte[] data, String fileName);

  FileSystemResource load(String fileName);

  void delete(String fileName);

  /**
   * Check if the file exists.
   * @param fileName the name of the file to check.
   */
  boolean exists(String fileName);

}
