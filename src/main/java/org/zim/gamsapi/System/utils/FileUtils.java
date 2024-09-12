package org.zim.gamsapi.System.utils;

import org.springframework.security.crypto.codec.Hex;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for file operations for GAMS5
 */
public class FileUtils {


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
   * TODO write tests for this (must create expected value!)
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


}
