package org.zim.gamsapi.Ingest.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.zim.gamsapi.Ingest.exceptions.IngestProcessingException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
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
        // check if first entry is null - migt be an invalid zip
        if(zipEntry == null) {
          String msg = "First zip entry in given ZIP bytes is null. Given byte[] is not a valid zipped directory.";
          log.error(msg);
          throw new IOException(msg);
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
      String msg = String.format("IOException at walking through the byte[] representation of given zipped directory. Make sure that given byte[] is a zipped directory! Got error: %s", e);
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

  }



  /**
   * Zips given directory to temporary file and returns the data as byte[].
   * @param sourceFile directory to zip.
   * @return zipped directory as byte[]
   */
  public static byte[] zipDir(File sourceFile) throws IngestProcessingException {

    if(!sourceFile.isDirectory()){
      String msg = String.format("Given file for zipping is not a directory! Got path %s represented via file %s", sourceFile.getAbsolutePath(), sourceFile);
      log.error(msg);
      throw new IngestProcessingException(msg);
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
      String msg = String.format("Failed to zip directory with path %s to temp-file. With reason: %s", sourceFile.getAbsolutePath(), e);
      log.error(msg);
      throw new IngestProcessingException(msg);
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
   * @param zippedDir TODO
   * @return path to temporary directory containing the now unzipped directory.
   * @throws IngestProcessingException if unzipping fails.
   */
  public static Path unzipToTempDir(byte[] zippedDir) throws IngestProcessingException {

    // first create random named temporary directory
    Path tempBagDirPath;

    try {
      //TODO think about this
      tempBagDirPath = Files.createTempDirectory(UUID.randomUUID().toString());
    } catch (IOException e){
      String msg = String.format("Failed to create root temporary directory during unzipping. Original error %s", e);
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

    // walk through zipped directory and create directories and files in temp directory
    ZipUtils.walkZippedDir(zippedDir, (zipEntry, byteArrayOutputStream) -> {
      Path tempFilePath = tempBagDirPath.resolve(zipEntry.getName());
      if(zipEntry.isDirectory()){
        try {
          Files.createDirectories(tempFilePath);
          log.info("Created temporary bag directory: {}", tempFilePath);
        } catch (IOException e) {
          String msg = String.format("Failed to create directory %s during unzipping. Original error %s", tempFilePath, e);
          log.error(msg);
          throw new IngestProcessingException(msg);
        }
      } else {
        try {
          // zip might contain entries like /datastreams/derla.sty1 --> need to create /datastreams/ directory first
          ensureParentDir(tempFilePath);
          Files.createFile(tempFilePath);
          Files.write(tempFilePath, byteArrayOutputStream.toByteArray());
          log.info("Successfully wrote file {} to temporary bag directory: {}", zipEntry.getName(), tempFilePath);
        } catch (IOException e) {
          String msg = String.format("Failed to create file %s during unzipping. Original error %s", tempFilePath, e);
          log.error(msg);
          throw new IngestProcessingException(msg);
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
        String msg = String.format("Failed to verify existence of parent directories of path: %s. Original error: %s", path, e);
        log.error(msg);
        throw new IngestProcessingException(msg);
      }

    }
  }

  /**
   * Deletes given directory and all its subdirectories and files.
   * @param dirPath path to directory to be deleted.
   * @throws IngestProcessingException if deletion fails through IOException.
   */
  public static void deleteDir(Path dirPath) throws IngestProcessingException {
    // delete temp directory last
    try (Stream<Path> entries = Files.walk(dirPath)){
      entries
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
      log.info("DELETED TEMP DIR: {}", dirPath);
    } catch (IOException e){
      String msg = String.format("Failed to delete temporary directory %s. Original error %s", dirPath, e);
      log.error(msg);
      throw new IngestProcessingException(msg);
    }
  }

}
