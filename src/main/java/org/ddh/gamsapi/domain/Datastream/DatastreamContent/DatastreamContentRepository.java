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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.stream.Stream;

@Repository
@Slf4j
public class DatastreamContentRepository implements IDatastreamContentRepository {

  private static final int BUFFER_SIZE = 8192; // 8KB buffer

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
              "Could not create root location for GAMS files at " + GAMS_FILES_ROOT + ". Original error: " + e.getMessage(),
              e
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
          "Failed to save datastream content. At balanced filepath: " + newFile + ". Datastream: " + datastreamId + ". Original error: " + e.getMessage(),
          e
      );
    }
  }

  /**
     * Saves data from an InputStream to filesystem.
     * Uses buffered streaming to minimize memory usage.
     *
     * @param inputStream the input stream to read from
     * @param datastreamId the datastream id
     * @return the datastream id
     * @throws IOException if file cannot be written
     */
  @Override
  public DatastreamId save(InputStream inputStream, DatastreamId datastreamId)
      throws IOException {

    if (!Files.exists(GAMS_FILES_ROOT)) {
      String msg = "Root location " + GAMS_FILES_ROOT +
          " does not exist for datastream " + datastreamId;
      throw new DatastreamCannotLoadFileException(msg);
    }

    Path targetFile = calcBalancedFilepath(datastreamId);

    // Ensure parent directories exist
    ZipUtils.ensureParentDir(targetFile);

    // Stream data with buffering - constant memory usage
    try (BufferedInputStream bis = new BufferedInputStream(inputStream, BUFFER_SIZE);
         BufferedOutputStream bos = new BufferedOutputStream(
             Files.newOutputStream(targetFile,
                 StandardOpenOption.CREATE,
                 StandardOpenOption.TRUNCATE_EXISTING,
                 StandardOpenOption.WRITE),
             BUFFER_SIZE)
    ) {

      byte[] buffer = new byte[BUFFER_SIZE];
      int bytesRead;

      while ((bytesRead = bis.read(buffer)) != -1) {
        bos.write(buffer, 0, bytesRead);
      }

      bos.flush();

    } catch (IOException e) {
      log.error("Failed to write file for datastream {}: {}",
          datastreamId, e.getMessage(), e);

      // Cleanup partial file on failure
      try {
        Files.deleteIfExists(targetFile);
      } catch (IOException cleanupEx) {
        log.warn("Failed to cleanup partial file {} after write error: {}",
            targetFile, cleanupEx.getMessage(), cleanupEx);
      }

      throw new DatastreamCannotWriteFileException(
          "Failed to save file for datastream " + datastreamId + " Original error: " + e.getMessage(),
          e
      );
    }

    return datastreamId;
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
          "Failed to find datastream file. For datastream: " + datastreamId + " From expected path: " + expectedPath +  ". Original error: " + e.getMessage(),
          e
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
      String msg = "Could not delete file at balanced filepath " + fileToDelete + ". For datastream-id: " + datastreamId + " Original error: " + e.getMessage();
      log.error(msg, e);
      throw new DatastreamCannotDeleteFileException(
          msg,
          e
      );
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
          "Could not hash file for datastream-id " + datastreamId + ". Original error: " + e.getMessage(),
          e
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
          throw new DatastreamCannotDeleteFileException(
              "Could not delete file " + path + " in GAMS. Original error: " + e.getMessage(),
              e
          );
        }
      });
    } catch (IOException e) {
      throw new DatastreamCannotDeleteFileException(
          "Could not delete all files in GAMS. Original error: " + e.getMessage(),
          e
      );
    }


  }


  /**
   * Saves data from an InputStream to filesystem while computing checksums.
   * Checksums are computed during the same stream read — zero extra I/O.
   */
  @Override
  public WriteResult saveWithChecksums(InputStream inputStream, DatastreamId datastreamId)
      throws IOException {

    if (!Files.exists(GAMS_FILES_ROOT)) {
      throw new DatastreamCannotLoadFileException(
          "Root location " + GAMS_FILES_ROOT +
              " does not exist for datastream " + datastreamId);
    }

    Path targetFile = calcBalancedFilepath(datastreamId);
    ZipUtils.ensureParentDir(targetFile);

    MessageDigest md5;
    MessageDigest sha512;
    try {
      md5 = MessageDigest.getInstance("MD5");
      sha512 = MessageDigest.getInstance("SHA-512");
    } catch (NoSuchAlgorithmException e) {
      // Should never happen — MD5 and SHA-512 are guaranteed by the JVM spec
      throw new IllegalStateException("Required digest algorithm not available", e);
    }

    try (BufferedInputStream bis = new BufferedInputStream(inputStream, BUFFER_SIZE);
         BufferedOutputStream bos = new BufferedOutputStream(
             Files.newOutputStream(targetFile,
                 StandardOpenOption.CREATE,
                 StandardOpenOption.TRUNCATE_EXISTING,
                 StandardOpenOption.WRITE),
             BUFFER_SIZE)) {

      byte[] buffer = new byte[BUFFER_SIZE];
      int bytesRead;

      while ((bytesRead = bis.read(buffer)) != -1) {
        md5.update(buffer, 0, bytesRead);
        sha512.update(buffer, 0, bytesRead);
        bos.write(buffer, 0, bytesRead);
      }

      bos.flush();

    } catch (IOException e) {
      log.error("Failed to write file for datastream {}: {}",
          datastreamId, e.getMessage(), e);

      try {
        Files.deleteIfExists(targetFile);
      } catch (IOException cleanupEx) {
        log.warn("Failed to cleanup partial file {} after write error: {}",
            targetFile, cleanupEx.getMessage(), cleanupEx);
      }

      throw new DatastreamCannotWriteFileException(
          "Failed to save file for datastream " + datastreamId +
              " Original error: " + e.getMessage(), e);
    }

    String md5Hex = HexFormat.of().formatHex(md5.digest());
    String sha512Hex = HexFormat.of().formatHex(sha512.digest());

    log.info("Successfully wrote datastream {} with checksums md5={}, sha512={}",
        datastreamId, md5Hex, sha512Hex);

    return new WriteResult(datastreamId, md5Hex, sha512Hex);
  }

  /**
   * Saves byte array to filesystem while computing checksums.
   */
  @Override
  public WriteResult saveWithChecksums(byte[] data, DatastreamId datastreamId) {

    if (!Files.exists(GAMS_FILES_ROOT)) {
      throw new DatastreamCannotLoadFileException(
          "Cannot write datastream file. Root location does not exist: " +
              GAMS_FILES_ROOT + " For datastream: " + datastreamId);
    }

    Path newFile = calcBalancedFilepath(datastreamId);
    ZipUtils.ensureParentDir(newFile);

    try {
      Files.write(newFile, data);
    } catch (IOException e) {
      throw new DatastreamCannotWriteFileException(
          "Failed to save datastream content at: " + newFile +
              " Datastream: " + datastreamId + " Error: " + e.getMessage(), e);
    }

    // Compute checksums over the byte array
    String md5Hex;
    String sha512Hex;
    try {
      md5Hex = HexFormat.of().formatHex(
          MessageDigest.getInstance("MD5").digest(data));
      sha512Hex = HexFormat.of().formatHex(
          MessageDigest.getInstance("SHA-512").digest(data));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Required digest algorithm not available", e);
    }

    log.info("Successfully wrote datastream {} with checksums md5={}, sha512={}",
        datastreamId, md5Hex, sha512Hex);

    return new WriteResult(datastreamId, md5Hex, sha512Hex);
  }

}
