package org.zim.gamsapi.Datastream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.crypto.codec.Hex;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
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
   * Save the given data to the given location.
   * TODO test
   * @param data the data to save
   * @param datastreamId the datastream id to save the data to
   * @return the path to the saved file
   */
  public Path save(byte[] data, DatastreamId datastreamId) {

    // error if root location does not exist
    if(!Files.exists(GAMS_FILES_ROOT)){
      String msg = String.format("No files stored in GAMS. The root location %s does not exist. For datastream with id %s", GAMS_FILES_ROOT, datastreamId);
      log.error(msg);
      throw new DatastreamCannotLoadFileException(msg);
    }


    Path newFile = calcBalancedFilepath(datastreamId.toString());

    // TODO refactor class to use FileUtils.ensureParentDir?
    ZipUtils.ensureParentDir(newFile);

    // TODO what happens if the file already exists? - and what should happen? (overwrite, error, ...)

    try {
      Files.write(newFile, data);
      log.info("Successfully wrote datastream {} with balanced path: {}", datastreamId, newFile);
      return newFile;
    } catch (Exception e) {
      String msg = String.format("Could not write datastream %s with balanced path: %s", datastreamId, newFile);
      log.error(msg, e);
      throw new DatastreamCannotWriteFileException(msg);
    }
  }

  /**
   * TODO test
   * @param datastreamId the datastream id to load the file for
   * @return the file system resource
   */
  public FileSystemResource load(DatastreamId datastreamId) {


    // TODO read: https://www.baeldung.com/java-read-lines-large-file

    // error if the root location does not exist
    // TODO I think this cannot happen (because gams is precreated)
    if(!Files.exists(GAMS_FILES_ROOT)){
      String msg = String.format("No files stored in GAMS. The root location %s does not exist. Tried to access file for datastream: %s", GAMS_FILES_ROOT, datastreamId);
      log.error(msg);
      throw new DatastreamCannotLoadFileException(msg);
    }

    // error if the file does not exist
    Path expectedPath = calcBalancedFilepath(datastreamId.toString());
    if(!Files.exists(expectedPath)){
      String msg = String.format("Cannot load datastream file. The file for datastream %s does not exist at path %s", datastreamId, expectedPath);
      log.error(msg);
      throw new DatastreamCannotLoadFileException(msg);
    }

    try {
      // TODO check if this is correct
      return new FileSystemResource(expectedPath);
    } catch (Exception e) {
      String msg = String.format("Could not load file for datastream %s from expected path %s. Original error: %s", datastreamId, expectedPath, e);
      log.error(msg);
      throw new DatastreamCannotLoadFileException(msg);
    }

  }


  /**
   * Hashes and balances given filename to a new location and deletes the result location.
   * @param datastreamId the datastream id to delete the file for
   * TODO test
   */
  public void delete(DatastreamId datastreamId) {
    Path fileToDelete = calcBalancedFilepath(datastreamId.toString());

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
      String msg = String.format("Could not delete file at balanced filepath %s. Original file/, datastream /name: %s Original error: %s", fileToDelete, datastreamId, e);
      // TODO handle exception
      log.error(msg);
      throw new DatastreamCannotDeleteFileException(msg);
    }
  }

  @Override
  public boolean exists(DatastreamId datastreamId) {
    //TODO test
    Path fileToCheck = calcBalancedFilepath(datastreamId.toString());
    return Files.exists(fileToCheck);
  }


  /**
   * Transforms given string to a sha256 hash --> and returns that as hex string
   * https://www.baeldung.com/sha-256-hashing-java
   * TODO think about good location? maybe move to other class!
   * TODO write tests for this (must create expected value!)
   * @return sha256 hash of given string as hex value
   */
  public String calcSha256Hex(String toHash){
    final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA3-256");
    } catch (NoSuchAlgorithmException e) {
      String msg = String.format("Could not create SHA3-256 digest for datastream-id: %s Original error: %s", this, e);
      log.error(msg);
      throw new DatastreamIdHashingException(msg);
    }

    final byte[] hashbytes = digest.digest(
        toHash.getBytes(StandardCharsets.UTF_8));

    char[] hex = Hex.encode(hashbytes);
    return String.valueOf(hex);
  }

  /**
   * Calculates the balanced filepath for the given filename for GAMS.
   * TODO doc + test
   * @param fileName
   * @return
   */
  public Path calcBalancedFilepath(String fileName){
    fileName = calcSha256Hex(fileName);
    fileName = FileUtils.balanceFilenameToFolderHierarchy(fileName, GAMS_FILE_BALANCE_FACTOR);
    return GAMS_FILES_ROOT.resolve(fileName);
  }

}
