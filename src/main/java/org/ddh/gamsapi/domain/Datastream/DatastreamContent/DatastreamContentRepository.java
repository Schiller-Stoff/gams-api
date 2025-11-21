package org.ddh.gamsapi.domain.Datastream.DatastreamContent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Repository;
import org.ddh.gamsapi.domain.Datastream.DatastreamId;
import org.ddh.gamsapi.domain.Datastream.utils.exceptions.DatastreamCannotDeleteFileException;
import org.ddh.gamsapi.domain.Datastream.utils.exceptions.DatastreamCannotLoadFileException;
import org.ddh.gamsapi.domain.Datastream.utils.exceptions.DatastreamCannotWriteFileException;
import org.ddh.gamsapi.domain.Datastream.utils.exceptions.DatastreamIdHashingException;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.ddh.gamsapi.application.Ingest.utils.ZipUtils;
import org.ddh.gamsapi.infrastructure.System.configproperties.GAMSStorageProperties;
import org.ddh.gamsapi.infrastructure.System.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.stream.Stream;

@Repository
@Slf4j
public class DatastreamContentRepository implements IDatastreamContentRepository {

   private final int GAMS_FILE_BALANCE_FACTOR = 16;

   public final Path GAMS_FILES_ROOT;

   public DatastreamContentRepository(GAMSStorageProperties gamsStorageProperties){
     GAMS_FILES_ROOT = Paths.get(gamsStorageProperties.getRootPath()).toAbsolutePath();

      // create root location if it does not exist
      if(!Files.exists(GAMS_FILES_ROOT)){
        try {
          Files.createDirectories(GAMS_FILES_ROOT);
          log.info("Created root location for GAMS files at {}", GAMS_FILES_ROOT);
        } catch (IOException e) {
          throw new DatastreamCannotWriteFileException(
              "Could not create root location for GAMS files at " + GAMS_FILES_ROOT + ". Original error: " + e
          );
        }
      }

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
      throw new DatastreamCannotLoadFileException(
          "Cannot write datastream file. The root location does not exist (and needs to be created first): " + GAMS_FILES_ROOT + " Make sure to correctly configure the gams-api application. For datastream with id " + datastreamId
      );
    }


    Path newFile = calcBalancedFilepath(datastreamId);

    ZipUtils.ensureParentDir(newFile);

    try {
      // without any options the file will be overwritten. (https://stackoverflow.com/questions/19794101/how-to-overwrite-file-via-java-nio-writer)
      Files.write(newFile, data);
      log.info("Successfully wrote datastream {} with balanced path: {}", datastreamId, newFile);
      return datastreamId;
    } catch (IOException e) {
      throw new DatastreamCannotWriteFileException(
          "Failed to save datastream content. At balanced filepath: " + newFile + ". Datastream: " + datastreamId + ". Original error: " + e
      );
    }
  }

  public InputStreamResource findById(DatastreamId datastreamId) {

    // error if the root location does not exist
    if(!Files.exists(GAMS_FILES_ROOT)){
      throw new DatastreamCannotLoadFileException(
          "Failed to find datastream file. The GAMS files root location does not exist unexpectedly: " + GAMS_FILES_ROOT + ". For datastream: " + datastreamId
      );
    }

    // error if the file does not exist
    Path expectedPath = calcBalancedFilepath(datastreamId);
    if(!Files.exists(expectedPath)){
      // TODO is this exception correct here? - a datastream content might not exist from perspective of this method
      // TODO new exception DatastreamContentNotFoundException ?
      throw new DatastreamCannotLoadFileException(
          "Failed to find datastream file. The expected file does not exist at path: " + expectedPath + ". For datastream: " + datastreamId
      );
    }

    try {
      return new InputStreamResource(new FileSystemResource(expectedPath).getInputStream());
    } catch (Exception e) {
      throw new DatastreamCannotLoadFileException(
          "Failed to find datastream file. For datastream: " + datastreamId + " From expected path: " + expectedPath +  ". Original error: " + e
      );
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
    Path fileToCheck = calcBalancedFilepath(datastreamId);
    return Files.exists(fileToCheck);
  }

  public Path calcBalancedFilepath(DatastreamId datastreamId){
    String hashedFileName;
    try {
      hashedFileName = FileUtils.calcSha256Hex(datastreamId.toString());
    } catch (NoSuchAlgorithmException e) {
      throw new DatastreamIdHashingException(
          "Could not hash file for datastream-id " + datastreamId + ". Original error: " + e
      );
    }

    String balamcedFileName = FileUtils.balanceFilenameToFolderHierarchy(hashedFileName, GAMS_FILE_BALANCE_FACTOR);
    return GAMS_FILES_ROOT.resolve(balamcedFileName);
  }

  public void deleteAll(){
    Path pathToBeDeleted = GAMS_FILES_ROOT.toAbsolutePath();
    try (Stream<Path> paths = Files.walk(pathToBeDeleted)) {
      paths.sorted(Comparator.reverseOrder()).forEach(path -> {
        if(path.getFileName().endsWith("README.md"))return;
        if(path.toAbsolutePath().equals(pathToBeDeleted))return;
        try {
          Files.delete(path);
        } catch (IOException e) {
          String msg = String.format("Could not delete file %s in GAMS. Original error: %s", path, e);
          log.error(msg);
          throw new DatastreamCannotDeleteFileException(msg);
        }
      });
    } catch (IOException e) {
      String msg = String.format("Could not delete all files in GAMS. Original error: %s", e);
      log.error(msg);
      throw new DatastreamCannotDeleteFileException(msg);
    }


  }

}
