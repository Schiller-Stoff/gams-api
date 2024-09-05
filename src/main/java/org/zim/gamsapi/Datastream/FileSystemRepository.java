package org.zim.gamsapi.Datastream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Repository;
import org.zim.gamsapi.Datastream.exceptions.DatastreamCannotLoadFileException;
import org.zim.gamsapi.Datastream.exceptions.DatastreamCannotWriteFileException;
import org.zim.gamsapi.Datastream.exceptions.DatastreamCannotDeleteFileException;
import org.zim.gamsapi.Datastream.interfaces.IFileSystemRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Repository
@Slf4j
public class FileSystemRepository implements IFileSystemRepository {


  // TODO make configurable
   public final String GAMS_ROOT_FOLDERNAME = "gams";

   public final Path GAMS_FILES_ROOT = accessGamsRootPath();


  /**
   * Returns the root of the GAMS files with exception handling
   * TODO test
   * @return the root of the GAMS files
   */
   private Path accessGamsRootPath(){
     // TODO use configured path if available!
     return Paths.get(GAMS_ROOT_FOLDERNAME).toAbsolutePath();
   }


  /**
   * Save the given data to the given location.
   * TODO test
   * @param data the data to save
   * @param fileName the relative location to save the data to
   * @return the path to the saved file
   */
  public Path save(byte[] data, String fileName) {

    // TODO build a folder hierarchy of the file!

    // https://stackoverflow.com/questions/1576272/storing-large-number-of-files-in-file-system
    // https://www.reddit.com/r/zfs/comments/uwabzd/best_method_to_store_40_million_files/

    // TODO think about storing a lot of files
    // https://stackoverflow.com/questions/1576272/storing-large-number-of-files-in-file-system


    // error if root location does not exist
    if(!Files.exists(GAMS_FILES_ROOT)){
      String msg = String.format("No files stored in GAMS. The root location %s does not exist. Tried to access file at location: %s", GAMS_FILES_ROOT, fileName);
      log.error(msg);
      throw new DatastreamCannotLoadFileException(msg);
    }


    Path newFile = GAMS_FILES_ROOT.resolve(fileName);

    // TODO what happens if the file already exists? - and what should happen? (overwrite, error, ...)

    try {
      Files.write(newFile, data);
      log.info("Successfully wrote file {}", newFile);
      return newFile;
    } catch (Exception e) {
      String msg = String.format("Could not write file %s", newFile);
      log.error(msg, e);
      throw new DatastreamCannotWriteFileException(msg);
    }
  }

  /**
   * TODO test
   * @param fileName the name of the file to load
   * @return the file system resource
   */
  public FileSystemResource load(String fileName) {

    // TODO read: https://www.baeldung.com/java-read-lines-large-file

    // error if the root location does not exist
    if(!Files.exists(GAMS_FILES_ROOT)){
      // TODO improve err msg
      String msg = String.format("No files stored in GAMS. The root location %s does not exist. Tried to access file: %s", GAMS_FILES_ROOT, fileName);
      log.error(msg);
      throw new DatastreamCannotLoadFileException(msg);
    }

    // error if the file does not exist
    Path expectedPath = GAMS_FILES_ROOT.resolve(fileName);
    if(!Files.exists(expectedPath)){
      String msg = String.format("Cannot load file. The file  %s does not exist at path %s", fileName, expectedPath);
      log.error(msg);
      throw new DatastreamCannotLoadFileException(msg);
    }

    try {
      // TODO check if this is correct
      return new FileSystemResource(expectedPath);
    } catch (Exception e) {
      String msg = String.format("Could not load file %s from expected path %s. Original error: %s", fileName, expectedPath, e);
      log.error(msg);
      throw new DatastreamCannotLoadFileException(msg);
    }

  }


  /**
   * Deletes the file with the given name.
   * TODO implement / think about
   * TODO test
   */
  public void delete(String fileName) {

    // TODO should not delete root directory!
    Path fileToDelete = GAMS_FILES_ROOT.resolve(fileName);

    try {
      Files.delete(fileToDelete);
      log.trace("Successfully deleted file {}", fileToDelete);
    } catch (IOException e) {
      String msg = String.format("Could not delete file %s. Original error: %s", fileToDelete, e);
      // TODO handle exception
      log.error(msg);
      throw new DatastreamCannotDeleteFileException(msg);
    }
  }

  @Override
  public boolean exists(String fileName) {
    //TODO test
    Path fileToCheck = GAMS_FILES_ROOT.resolve(fileName);
    return Files.exists(fileToCheck);

  }
}
