package org.zim.gamsapi.Datastream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Repository;
import org.zim.gamsapi.Datastream.exceptions.DatastreamCannotLoadFileException;
import org.zim.gamsapi.Datastream.exceptions.DatastreamCannotWriteFileException;
import org.zim.gamsapi.Datastream.exceptions.DatastreamCannotDeleteFileException;
import org.zim.gamsapi.Datastream.exceptions.DatastreamIdHashingException;
import org.zim.gamsapi.Datastream.interfaces.IFileSystemRepository;
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
public class FileSystemRepository implements IFileSystemRepository {


   private final int GAMS_FILE_BALANCE_FACTOR = 16;

   public final Path GAMS_FILES_ROOT;

   public FileSystemRepository(GAMSStorageProperties gamsStorageProperties){
     GAMS_FILES_ROOT = Paths.get(gamsStorageProperties.getRootPath()).toAbsolutePath();
   }


  /**
   * Save the given data to the given location.
   * TODO test
   * @param data the data to save
   * @param fileName the relative location to save the data to
   * @return the path to the saved file
   */
  public Path save(byte[] data, String fileName) {

    // error if root location does not exist
    if(!Files.exists(GAMS_FILES_ROOT)){
      String msg = String.format("No files stored in GAMS. The root location %s does not exist. Tried to access file at location: %s", GAMS_FILES_ROOT, fileName);
      log.error(msg);
      throw new DatastreamCannotLoadFileException(msg);
    }


    Path newFile = calcBalancedFilepath(fileName);

    // TODO refactor class to use FileUtils.ensureParentDir?
    ZipUtils.ensureParentDir(newFile);

    // TODO what happens if the file already exists? - and what should happen? (overwrite, error, ...)

    try {
      Files.write(newFile, data);
      log.info("Successfully wrote file {} with balanced path: {}", fileName, newFile);
      return newFile;
    } catch (Exception e) {
      String msg = String.format("Could not write file %s with balanced path: %s", fileName, newFile);
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
    // TODO I think this cannot happen (because gams is precreated)
    if(!Files.exists(GAMS_FILES_ROOT)){
      String msg = String.format("No files stored in GAMS. The root location %s does not exist. Tried to access file: %s", GAMS_FILES_ROOT, fileName);
      log.error(msg);
      throw new DatastreamCannotLoadFileException(msg);
    }

    // error if the file does not exist
    Path expectedPath = calcBalancedFilepath(fileName);
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
   * Hashes and balances given filename to a new location and deletes the result location.
   * @param fileName the name of the file to load
   * TODO test
   */
  public void delete(String fileName) {
    Path fileToDelete = calcBalancedFilepath(fileName);

    // extra assertion for debug reason
    if(!Files.exists(fileToDelete)){
      String msg = String.format("Could not delete file at balanced filepath %s. Original filename: %s It doesn't exist", fileToDelete, fileName);
      log.error(msg);
      throw new DatastreamCannotDeleteFileException(msg);
    }

    try {
      Files.delete(fileToDelete);
      log.trace("Successfully deleted file with name {} at balanced location {}", fileName, fileToDelete);
    } catch (IOException e) {
      String msg = String.format("Could not delete file at balanced filepath %s. Original filename: %s Original error: %s", fileToDelete, fileName, e);
      // TODO handle exception
      log.error(msg);
      throw new DatastreamCannotDeleteFileException(msg);
    }
  }

  @Override
  public boolean exists(String fileName) {
    //TODO test
    Path fileToCheck = calcBalancedFilepath(fileName);
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
