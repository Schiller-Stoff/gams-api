package org.ddh.gamsapi.infrastructure.System.utils;

import org.springframework.security.crypto.codec.Hex;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility class for file operations for GAMS5
 */
public class FileUtils {

  private FileUtils() {
    /* This utility class should not be instantiated */
    throw new IllegalStateException("Utility class should not be instantiated");
  }



  /**
   * Splits given string every n characters
   * @param text String to be split
   * @param n number of characters to split by
   * @return List of strings
   */
  public static List<String> splitStringByN(String text, int n){
    List<String> results = new ArrayList<>();
    int length = text.length();

    for (int i = 0; i < length; i += n) {
      results.add(text.substring(i, Math.min(length, i + n)));
    }

    return results;
  }


  /**
   * Balances the filename to a folder hierarchy e.g. "filename.txt" -> "fi/le/na/me.txt"
   * according to a given balancing factor which decides on the levels of holder hierarchies.
   * On filepaths: https://www.baeldung.com/java-file-vs-file-path-separator
   * @param filename the filename to balance
   * @param n the number of characters to split by
   * @return the balanced folder hierarchy as String
   */
  public static String balanceFilenameToFolderHierarchy(String filename, int n){
    if(filename.contains(".")){
      String[] parts = filename.split("\\.");
      String extension = parts[parts.length - 1];
      filename = parts[0];
      filename = String.join(File.separator, FileUtils.splitStringByN(filename, n));
      return filename + "." + extension;
    }
    return String.join(File.separator, FileUtils.splitStringByN(filename, n));
  }


  /**
   * Transforms given string to a sha256 hash --> and returns that as hex string
   * https://www.baeldung.com/sha-256-hashing-java
   * @return sha256 hash of given string as hex value
   * @throws NoSuchAlgorithmException if the algorithm is not found
   */
  public static String calcSha256Hex(String toHash) throws NoSuchAlgorithmException{
    final MessageDigest digest = MessageDigest.getInstance("SHA3-256");
    final byte[] hashbytes = digest.digest(
        toHash.getBytes(StandardCharsets.UTF_8));
    char[] hex = Hex.encode(hashbytes);
    return String.valueOf(hex);
  }

  /**
   * Empties given directory + allows to skip certain files.
   * @param directoryToBeEmptied directory to be emptied
   * @param skipPaths paths to be skipped from deletion
   */
  public static void emptyDirectory(File directoryToBeEmptied, Set<String> skipPaths) throws IOException {
    emptyDirectory(directoryToBeEmptied, directoryToBeEmptied.toPath(), skipPaths);
  }

  /**
   * Empties given directory.
   * @param directoryToBeEmptied directory to be emptied
   */
  public static void emptyDirectory(File directoryToBeEmptied) throws IOException {
    emptyDirectory(directoryToBeEmptied, Set.of());
  }

  /**
   * Empties given directory
   * @param current current iterated file
   * @param root root of the directory
   * @param skipPaths paths to skip
   */
  private static void emptyDirectory(File current, Path root, Set<String> skipPaths) throws IOException {
    File[] allContents = current.listFiles();
    if (allContents != null) {
      for (File file : allContents) {
        // also manage windows paths
        String relativePath = root.relativize(file.toPath()).toString().replace('\\', '/');

        if (skipPaths.contains(relativePath)) {
          continue;
        }

        if (file.isDirectory()) {
          emptyDirectory(file, root, skipPaths);
        }
        file.delete();
      }
    }
  }


}
