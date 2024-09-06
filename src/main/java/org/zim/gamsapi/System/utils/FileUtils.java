package org.zim.gamsapi.System.utils;

import java.io.File;
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




}
