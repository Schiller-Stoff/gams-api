package org.ddh.gamsapi.application.Ingest.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.InputMismatchException;

/**
 * Provides methods to detect mimetypes etc. from inputstreams, files and filenames.
 * Not intended to work very reliably.
 */
public class MimeTypeDetector {

  /**
   * Extracts mimetype as string from given input stream.
   * 
   * @param inputStream {InputStream} where the mimetype should be extracted.
   * @return {String} detected mimetype as string.
   * @throws IOException            Failed to interpret input-stream.
   * @throws InputMismatchException Failed to read in inputstream or failed to
   *                                detect mimetype.
   */
  public static String detect(InputStream inputStream) throws IOException {

    String detectedMimetype = URLConnection.guessContentTypeFromStream(inputStream);

    if (detectedMimetype == null) {
      throw new IOException("Unable to detect mimetype of given input-stream");
    }

    return detectedMimetype;

  }

  /**
   * Extracts mimetype as string from path
   * 
   * @param fileName {String} where the mimetype should be extracted.
   * @return {String} detected mimetype as string.
   * @throws IOException            Failed to interpret input-stream.
   * @throws InputMismatchException Failed to read in inputstream or failed to
   *                                detect mimetype.
   */
  public static String detect(String fileName) throws IOException {

    String detectedMimetype = URLConnection.guessContentTypeFromName(fileName);

    if (detectedMimetype == null) {
      throw new IOException("Unable to detect mimetype of given input-stream");
    }

    return detectedMimetype;

  }


  /**
   * Returns valid mimetype string in any case. 
   * First tries to detect mimetype from given mimetype if failing -> reads tries to interpret 
   * the file name.
   * If none detected returns "application/octet-stream".
   * @return {String} Mimetype of given data. 
   */
  public static String forceDetect(InputStream inputStream, String fileName){
    
    String detectedMimetype = "";

    try {
       detectedMimetype = detect(inputStream);
    } catch (IOException e){
      try {
        detectedMimetype = detect(fileName);
      } catch (IOException ioe){
        detectedMimetype = "application/octet-stream";
      }
      
    }

    return detectedMimetype;

  }

}
