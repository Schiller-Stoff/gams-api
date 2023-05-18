package org.zim.gamsapi.SubInfoPack.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.zim.gamsapi.SubInfoPack.exceptions.SubInfoPackProcessingException;
import java.io.*;
import java.nio.file.Files;
import java.util.function.BiConsumer;
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
   */
  public static void walkZippedDir(byte[] zippedDir, BiConsumer<ZipEntry, ByteArrayOutputStream> consumer) throws SubInfoPackProcessingException {
    try {
      try(ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zippedDir))){
        ZipEntry zipEntry = zipInputStream.getNextEntry();
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
      throw new SubInfoPackProcessingException(msg);
    }

  }



  /**
   * Zips given directory to temporary file and returns the data as byte[].
   * @param sourceFile directory to zip.
   * @return zipped directory as byte[]
   */
  public static byte[] zipDir(File sourceFile) throws SubInfoPackProcessingException {

    if(!sourceFile.isDirectory()){
      String msg = String.format("Given file for zipping is not a directory! Got path %s represented via file %s", sourceFile.getAbsolutePath(), sourceFile);
      log.error(msg);
      throw new SubInfoPackProcessingException(msg);
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
      throw new SubInfoPackProcessingException(msg);
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
    ZipEntry zipEntry = new ZipEntry(fileName);
    zipOut.putNextEntry(zipEntry);
    byte[] bytes = new byte[1024];
    int length;
    while ((length = fis.read(bytes)) >= 0) {
      zipOut.write(bytes, 0, length);
    }
    fis.close();
  }


}
