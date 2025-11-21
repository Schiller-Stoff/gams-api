package org.ddh.gamsapi.application.Ingest.utils;

import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Ingest.exceptions.IngestProcessingException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


/**
 * Utility class for handling zip related operations, like unzipping and zipping
 * of folders.
 * - Allows to work based on byte[]
 */
@Slf4j
public class ZipUtils {

  /**
   * Iterates over parsed zippedDirectory and calls passed in lambda function - providing individual
   * zip entries as input.
   * @param zippedDir zipped directory as byte[]
   * @param consumer Function to be called on looped directory contents
   * @throws IngestProcessingException if zippedDir is not a valid zip (contains no entries) or if an IOException has been thrown during processing.
   */
  public static void walkZippedDir(byte[] zippedDir, BiConsumer<ZipEntry, ByteArrayOutputStream> consumer) throws IngestProcessingException {
    try {
      try(ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zippedDir))){
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        // check if first entry is null - might be an invalid zip
        if(zipEntry == null) {
          throw new IOException(
              "First zip entry in given ZIP bytes is null. Given byte[] is not a valid zipped directory."
          );
        }
        // go through all entries
        while (zipEntry != null) {
          // https://stackoverflow.com/questions/65322025/how-to-extract-zip-file-in-memory
          ByteArrayOutputStream outStream = new ByteArrayOutputStream();
          byte[] buffer = new byte[1024];
          int length;
          while ((length = zipInputStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, length);
          }
          consumer.accept(zipEntry, outStream);
          zipEntry = zipInputStream.getNextEntry();
        }
      }
    } catch (IOException e){
      throw new IngestProcessingException(
          "IOException at walking through the stream representation of given zipped directory. Make sure that given stream is a zipped directory! Got error: " + e
      );
    }

  }



  /**
   * Zips given directory to temporary file and returns the data as byte[].
   * @param sourceFile directory to zip.
   * @return zipped directory as byte[]
   */
  public static byte[] zipDir(File sourceFile) throws IngestProcessingException {

    if(!sourceFile.isDirectory()){
      throw new IngestProcessingException(
          "Given file for zipping is not a directory! Got path " + sourceFile.getAbsolutePath() + "represented via file " + sourceFile
      );
    }

    try {
      File tempFile = File.createTempFile("ingest", ".zip");
      tempFile.deleteOnExit();
      FileOutputStream fos = new FileOutputStream(tempFile);
      ZipOutputStream zipOut = new ZipOutputStream(fos);
      zipFile(sourceFile, sourceFile.getName(), zipOut);
      zipOut.close();
      fos.close();
      return Files.readAllBytes(tempFile.toPath());
    } catch (IOException e){
      throw new IngestProcessingException(
          "Failed to zip directory to temp-file. At path: " + sourceFile.getAbsolutePath() + " With reason: " + e
      );
    }

  }

  /**
   * Loops through direct content and zips it's content.
   * @param fileToZip file to zip
   * @param fileName filename
   * @param zipOut output stream
   * @throws IOException error at zipping e.g. file not found
   */
  private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
    if (fileToZip.isHidden()) {
      return;
    }
    if (fileToZip.isDirectory()) {
      File[] children = fileToZip.listFiles();
      if(children != null){
        for (File childFile : children) {
          zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
        }
        return;
      }
    }
    FileInputStream fis = new FileInputStream(fileToZip);
    // removes the folder name as root from the individual zip entry
    // otherwise the fileToZip would be the root of the zip (and not it's content!)
    String removedRoot = fileName.substring(fileName.indexOf("/") + 1);
    ZipEntry zipEntry = new ZipEntry(removedRoot);
    zipOut.putNextEntry(zipEntry);
    byte[] bytes = new byte[1024];
    int length;
    while ((length = fis.read(bytes)) >= 0) {
      zipOut.write(bytes, 0, length);
    }
    fis.close();
  }


  /**
   * Unzips a zipped directory to a temporary directory.
   * @param zippedDir zipped directory as byte[]
   * @return path to temporary directory containing the now unzipped directory.
   * @throws IngestProcessingException if unzipping fails.
   */
  public static Path unzipToTempDir(byte[] zippedDir) throws IngestProcessingException {

    // first create random named temporary directory
    Path tempBagDirPath;

    try {
      //TODO think about this2
      tempBagDirPath = Files.createTempDirectory(UUID.randomUUID().toString());
    } catch (IOException e){
      throw new IngestProcessingException(
          "Failed to create root temporary directory during unzipping. Original error " + e
      );
    }

    // walk through zipped directory and create directories and files in temp directory
    ZipUtils.walkZippedDir(zippedDir, (zipEntry, byteArrayOutputStream) -> {
      Path tempFilePath = tempBagDirPath.resolve(zipEntry.getName());
      if(zipEntry.isDirectory()){
        try {
          Files.createDirectories(tempFilePath);
          log.info("Created temporary bag directory: {}", tempFilePath);
        } catch (IOException e) {
          throw new IngestProcessingException(
              "Failed to create directory during unzipping. At path: " + tempFilePath + " Original error: " + e
          );
        }
      } else {
        try {
          // zip might contain entries like /datastreams/demo.1 --> need to create /datastreams/ directory first
          ensureParentDir(tempFilePath);
          Files.createFile(tempFilePath);
          Files.write(tempFilePath, byteArrayOutputStream.toByteArray());
          log.info("Successfully wrote file {} to temporary bag directory: {}", zipEntry.getName(), tempFilePath);
        } catch (IOException e) {
          throw new IngestProcessingException(
              "Failed to create file during unzipping. Filepath: " + tempFilePath + " Original error " + e
          );
        }
      }
    });

    return tempBagDirPath;
  }

  /**
   * Makes sure that all parent directories of the given path exist.
   * @param path path to check
   * @throws IngestProcessingException if missing parent directories cannot be created
   */
  public static void ensureParentDir(Path path) throws IngestProcessingException {
    if(Files.exists(path.getParent())){
      return;
    } else {
      try {
        // recursively call itself until parent directory exists
        ensureParentDir(path.getParent());
        Files.createDirectory(path.getParent());
      } catch (IOException e){
        throw new IngestProcessingException(
            "Failed to verify existence of parent directories from path: " + path + ". Original error: " + e
        );
      }

    }
  }

  /**
   * Deletes given directory and all its subdirectories and files.
   * Throws exception if any deletion fails.
   *
   * @param dirPath path to directory to be deleted
   * @throws IngestProcessingException if deletion fails or directory doesn't exist
   */
  public static void deleteDir(Path dirPath) throws IngestProcessingException {

    if (!Files.exists(dirPath)) {
      throw new IngestProcessingException(
          "Cannot delete directory - path does not exist: " + dirPath
      );
    }

    List<Path> failedDeletions = new ArrayList<>();

    try (Stream<Path> entries = Files.walk(dirPath)) {
      entries
          .sorted(Comparator.reverseOrder())
          .forEach(path -> {
            try {
              Files.delete(path);
              log.trace("Deleted: {}", path);
            } catch (IOException e) {
              log.warn("Failed to delete path: {} - Reason: {}", path, e.getMessage());
              failedDeletions.add(path);
            }
          });
    } catch (IOException e) {
      throw new IngestProcessingException(
          "Failed to walk directory tree for deletion: " + dirPath + ". Original error: " + e
      );
    }

    if (!failedDeletions.isEmpty()) {
      String failedPaths = failedDeletions.stream()
          .map(Path::toString)
          .collect(Collectors.joining(", "));
      throw new IngestProcessingException(
          "Failed to delete " + failedDeletions.size() + " path(s) in directory " + dirPath
              + ". Failed paths: " + failedPaths
      );
    }

    log.trace("Successfully deleted temporary directory: {}", dirPath);
  }


  /**
   * Unzips from input stream directly to temporary directory.
   * Memory-efficient - streams data without loading entire zip into memory.
   * @param zipInputStream input stream of zipped data
   * @return path to temporary directory containing unzipped content
   * @throws IngestProcessingException if unzipping fails
   */
  public static Path unzipStreamToTempDir(InputStream zipInputStream)
      throws IngestProcessingException {

    Path tempBagDirPath;
    try {
      // TODO think about prefix for temp directory
      // Creates UNIQUE directory - Java appends random suffix automatically
      tempBagDirPath = Files.createTempDirectory("gams-ingest-");
    } catch (IOException e) {
      throw new IngestProcessingException(
          "Failed to create temporary directory: " + e.getMessage()
      );
    }

    try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
      ZipEntry zipEntry = zis.getNextEntry();

      if (zipEntry == null) {
        throw new IngestProcessingException("Invalid zip file: no entries found");
      }

      while (zipEntry != null) {
        Path targetPath = tempBagDirPath.resolve(zipEntry.getName());

        // Security: prevent path traversal attacks
        if (!targetPath.normalize().startsWith(tempBagDirPath.normalize())) {
          throw new IngestProcessingException(
              "Invalid zip entry: " + zipEntry.getName() + " (path traversal detected)"
          );
        }

        if (zipEntry.isDirectory()) {
          Files.createDirectories(targetPath);
        } else {
          // Ensure parent directories exist
          Files.createDirectories(targetPath.getParent());

          // Stream directly to file (no intermediate byte[])
          Files.copy(zis, targetPath);
          log.trace("Extracted: {}", zipEntry.getName());
        }

        zis.closeEntry();
        zipEntry = zis.getNextEntry();
      }
    } catch (IOException e) {
      try {
        deleteDir(tempBagDirPath);
      } catch (IngestProcessingException cleanupEx) {
        log.warn("Failed to cleanup after unzip error", cleanupEx);
      }
      // CRITICAL: Always throw original exception
      throw new IngestProcessingException(
          "Failed to unzip stream: " + e.getMessage()
      );
    }

    log.trace("Unzipped to temporary directory: {}", tempBagDirPath);
    return tempBagDirPath;
  }

}
