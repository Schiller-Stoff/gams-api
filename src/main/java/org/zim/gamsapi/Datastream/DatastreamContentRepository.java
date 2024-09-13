package org.zim.gamsapi.Datastream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Repository;
import org.zim.gamsapi.Datastream.exceptions.DatastreamCannotLoadFileException;
import org.zim.gamsapi.Datastream.exceptions.DatastreamCannotWriteFileException;
import org.zim.gamsapi.Datastream.exceptions.DatastreamCannotDeleteFileException;
import org.zim.gamsapi.Datastream.exceptions.DatastreamIdHashingException;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.Ingest.utils.ZipUtils;
import org.zim.gamsapi.System.configproperties.GAMSStorageProperties;
import org.zim.gamsapi.System.utils.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;

@Repository
@Slf4j
public class DatastreamContentRepository implements IDatastreamContentRepository {

   private final int GAMS_FILE_BALANCE_FACTOR = 16;

   public final Path GAMS_FILES_ROOT;

   public DatastreamContentRepository(GAMSStorageProperties gamsStorageProperties){
     GAMS_FILES_ROOT = Paths.get(gamsStorageProperties.getRootPath()).toAbsolutePath();
   }


  /**
   * Saves given data.
   * @param data the data to save
   * @param datastreamId the datastream id to save the data to
   * @return given datastream id
   */
  public DatastreamId save(byte[] data, DatastreamId datastreamId) {

    // error if root location does not exist
    if(!Files.exists(GAMS_FILES_ROOT)){
      String msg = String.format("No files stored in GAMS. The root location %s does not exist. For datastream with id %s", GAMS_FILES_ROOT, datastreamId);
      log.error(msg);
      throw new DatastreamCannotLoadFileException(msg);
    }


    Path newFile = calcBalancedFilepath(datastreamId);

    ZipUtils.ensureParentDir(newFile);

    try {
      // without any options the file will be overwritten. (https://stackoverflow.com/questions/19794101/how-to-overwrite-file-via-java-nio-writer)
      Files.write(newFile, data);
      log.info("Successfully wrote datastream {} with balanced path: {}", datastreamId, newFile);
      return datastreamId;
    } catch (IOException e) {
      String msg = String.format("Could not write datastream %s with balanced path: %s", datastreamId, newFile);
      log.error(msg, e);
      throw new DatastreamCannotWriteFileException(msg);
    }
  }

  public InputStreamResource findById(DatastreamId datastreamId) {

    // TODO write test

    // TODO read: https://www.baeldung.com/java-read-lines-large-file

    // error if the root location does not exist
    if(!Files.exists(GAMS_FILES_ROOT)){
      String msg = String.format("No files stored in GAMS. The root location %s does not exist. Tried to access file for datastream: %s", GAMS_FILES_ROOT, datastreamId);
      log.error(msg);
      throw new DatastreamCannotLoadFileException(msg);
    }

    // error if the file does not exist
    Path expectedPath = calcBalancedFilepath(datastreamId);
    if(!Files.exists(expectedPath)){
      String msg = String.format("Cannot load datastream file. The file for datastream %s does not exist at path %s", datastreamId, expectedPath);
      log.error(msg);
      throw new DatastreamCannotLoadFileException(msg);
    }

    try {
      return new InputStreamResource(new FileSystemResource(expectedPath).getInputStream());
    } catch (Exception e) {
      String msg = String.format("Could not load file for datastream %s from expected path %s. Original error: %s", datastreamId, expectedPath, e);
      log.error(msg);
      throw new DatastreamCannotLoadFileException(msg);
    }

  }


  /**
   * Hashes and balances given filename to a new location and deletes the result location.
   * @param datastreamId the datastream id to delete the file for
   */
  public void delete(DatastreamId datastreamId) {
    Path fileToDelete = calcBalancedFilepath(datastreamId);

    // extra assertion for debug reason
    if(!Files.exists(fileToDelete)){
      String msg = String.format("Could not delete file at balanced filepath %s. For datastream: %s File doesn't exist", fileToDelete, datastreamId);
      log.error(msg);
      throw new DatastreamCannotDeleteFileException(msg);
    }

    try {
      Files.delete(fileToDelete);
      log.trace("Successfully deleted file for datastream {} at balanced location {}", datastreamId, fileToDelete);
    } catch (IOException e) {
      String msg = String.format("Could not delete file at balanced filepath %s. For datastream-id: %s Original error: %s", fileToDelete, datastreamId, e);
      log.error(msg);
      throw new DatastreamCannotDeleteFileException(msg);
    }
  }

  @Override
  public boolean exists(DatastreamId datastreamId) {
    //TODO test
    Path fileToCheck = calcBalancedFilepath(datastreamId);
    return Files.exists(fileToCheck);
  }

  public Path calcBalancedFilepath(DatastreamId datastreamId){
    String hashedFileName;
    try {
      hashedFileName = FileUtils.calcSha256Hex(datastreamId.toString());
    } catch (NoSuchAlgorithmException e) {
      String msg = String.format("Could not hash file for datastream-id %s. Original error: %s", datastreamId, e);
      log.error(msg);
      throw new DatastreamIdHashingException(msg);
    }

    String balamcedFileName = FileUtils.balanceFilenameToFolderHierarchy(hashedFileName, GAMS_FILE_BALANCE_FACTOR);
    return GAMS_FILES_ROOT.resolve(balamcedFileName);
  }

}
