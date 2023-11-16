package org.zim.gamsapi.SubInfoPack.utils;

import lombok.extern.slf4j.Slf4j;
import org.zim.gamsapi.SubInfoPack.exceptions.SubInfoPackProcessingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Utility class for BagIt related operations.
 */
@Slf4j
public class BagitUtils {


  /**
   * Maps the key value pairs in the bag-info.txt file to a BagItInfo object.
   * @param bagItDirPath The path to the bagit directory.
   * @return A BagItInfo object.
   * @throws SubInfoPackProcessingException If the bag-info.txt file is missing or if a required key is missing.
   */
  public static BagItInfo mapBagItInfo(Path bagItDirPath) throws SubInfoPackProcessingException {

    String pathToBagInfoFile = bagItDirPath.resolve(BagItFilePaths.BAG_INFO_FILE_PATH.name).toString();
    Map<String, String> fileValues = mapKeyValueTextFile(pathToBagInfoFile);

    try {
      return  BagItInfo.builder()
              .id(fileValues.get("Id"))
              .title(fileValues.get("Title"))
              .contactMail(fileValues.get("Contact-Email"))
              .type(fileValues.get("Type"))
              .externalDescription(fileValues.get("External-Description"))
              .publisher(fileValues.get("Publisher"))
              .rights(fileValues.get("Rights"))
              .creator(fileValues.get("Creator"))
              .childObjectIds(fileValues.get("Child-Object-Ids"))
              .build();
    } catch(NullPointerException e){
      String msg = String.format("Failed to extract a required key from %s to intern BagItInfo class. Original error: %s", BagItFilePaths.BAG_INFO_FILE_PATH.name, e);
      log.error(msg);
      throw new SubInfoPackProcessingException(msg);
    }

  }



  /**
   * Maps the key value pairs in defined text file to a map.
   * @param filePath The path to the text file.
   * @return A map of the key value pairs in the text file.
   */
  private static Map<String, String> mapKeyValueTextFile(String filePath) throws SubInfoPackProcessingException {
    Map<String, String> map = new HashMap<>();
    try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
      lines.filter(line -> line.contains(":"))
              .forEach(line -> {
                String[] keyValuePair = line.split(":", 2);
                String key = keyValuePair[0];
                String value = keyValuePair[1];
                map.put(key, value);
              });
    } catch (IOException e) {
      String msg = String.format("Failed to map key value pairs in file %s to map. Original error: %s", filePath, e);
      log.error(msg);
      throw new SubInfoPackProcessingException(msg);
    }
    return map;
  }

}
