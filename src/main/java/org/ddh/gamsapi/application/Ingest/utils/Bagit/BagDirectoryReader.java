package org.ddh.gamsapi.application.Ingest.utils.Bagit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Ingest.exceptions.IngestBagDuplicatedChecksumException;
import org.ddh.gamsapi.domain.Datastream.utils.GAMSDsid;
import org.ddh.gamsapi.application.Ingest.exceptions.IngestProcessingException;
import org.ddh.gamsapi.application.Ingest.utils.Bagit.mapping.BagSipJsonContentFile;
import org.ddh.gamsapi.application.Ingest.utils.Bagit.mapping.BagSipJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for BagIt related operations.
 */
@Slf4j
public class BagDirectoryReader {

  /**
   * Reads the sha512 manifest file and returns a map of bag paths to sha512 checksums.
   * @param bagDirPath path to the bag directory
   * @return Map of bagPaths (keys) to sha512 checksums (values)
   * @throws IngestProcessingException if the manifest file is missing or malformed (checksums not 128 characters long, duplicate checksums, empty bag paths)
   */
  public static Map<String, String> readSha512ManifestFile(Path bagDirPath) throws IngestProcessingException {
    String pathToManifestFile = bagDirPath.resolve(BagFilePaths.MANIFEST_SHA512_FILE_PATH.name).toString();

    var checksumPathsMap = readKeyValueTxtFile(pathToManifestFile, " ");

    var bagPathChecksumMap = new HashMap<String, String>();
    checksumPathsMap.forEach(
        (sha512Checksum, bagPath) -> {
           if(bagPathChecksumMap.containsValue(sha512Checksum)){
            throw new IngestProcessingException(
                "Encountered duplicate checksum" + sha512Checksum + "in sha512 manifest file " + BagFilePaths.MANIFEST_SHA512_FILE_PATH.name
            );
           }
           if(sha512Checksum.length() != 128) {
            throw new IngestProcessingException(
                "Encountered invalid sha512 checksum " + sha512Checksum + " in sha512 manifest file " + BagFilePaths.MANIFEST_SHA512_FILE_PATH.name + ". Checksum must be 128 characters long."
            );
           }

          if(bagPath == null){
            throw new IngestProcessingException(
                "Encountered null bag path for checksum " + sha512Checksum + " in sha512 manifest file " + BagFilePaths.MANIFEST_MD5_FILE_PATH.name + "."
            );
          }

          if(bagPath.isBlank()){
            throw new IngestProcessingException(
                "Encountered empty bag path for checksum " + sha512Checksum + " in sha512 manifest file " + BagFilePaths.MANIFEST_MD5_FILE_PATH.name + "."
            );
          }

           bagPathChecksumMap.put(bagPath, sha512Checksum);
        }
    );

    return bagPathChecksumMap;
  }

  /**
   * Reads the md5 manifest file and returns a map of bag paths to md5 checksums.
   * @param bagDirPath path to the bag directory
   * @return Map of bagPaths (keys) to md5 checksums (values)
   * @throws IngestProcessingException if the manifest file is missing or malformed (checksums not 32 characters long, duplicate checksums, empty bag paths)
   */
  public static Map<String,String> readMd5ManifestFile(Path bagDirPath) throws IngestProcessingException {
    String pathToManifestFile = bagDirPath.resolve(BagFilePaths.MANIFEST_MD5_FILE_PATH.name).toString();

    var checksumPathsMap = readKeyValueTxtFile(pathToManifestFile, " ");

    var bagPathChecksumMap = new HashMap<String, String>();
    checksumPathsMap.forEach(
        (checksum, bagPath) -> {
          if(bagPathChecksumMap.containsKey(checksum)){
            throw new IngestProcessingException(
                "Encountered duplicate checksum " + checksum + " in md5 manifest file " + BagFilePaths.MANIFEST_MD5_FILE_PATH.name + "."
            );
          }
          if(checksum.length() != 32) {
            throw new IngestProcessingException(
                "Encountered invalid md5 checksum " + checksum + " in md5 manifest file " + BagFilePaths.MANIFEST_SHA512_FILE_PATH.name + ". Checksum must be 32 characters long."
            );
          }

          if(bagPath == null){
            throw new IngestProcessingException(
                "Encountered null bag path for checksum " + checksum + " in md5 manifest file " + BagFilePaths.MANIFEST_MD5_FILE_PATH.name + "."
            );
          }

          if(bagPath.isBlank()){
            throw new IngestProcessingException(
                "Encountered empty bag path for checksum " + checksum + " in md5 manifest file " + BagFilePaths.MANIFEST_MD5_FILE_PATH.name + "."
            );
          }

          bagPathChecksumMap.put(bagPath, checksum);
        }
    );
    return bagPathChecksumMap;
  }

  /**
   * Maps the key value pairs in the bag-info.txt file to a BagInfo object.
   * @param bagDirPath path to the bag directory.
   * @return A BagItInfo object.
   * @throws IngestProcessingException If the bag-info.txt file is missing or if a required key is missing.
   */
  public static BagInfo readBagInfoFile(Path bagDirPath) throws IngestProcessingException {

    String pathToBagInfoFile = bagDirPath.resolve(BagFilePaths.BAG_INFO_FILE_PATH.name).toString();
    Map<String, String> fileValues = readKeyValueTxtFile(pathToBagInfoFile);

    BagInfo bagInfo;

    try {
      bagInfo = BagInfo.builder()
              .date(fileValues.get("Bagging-Date"))
              .payloadOxum(fileValues.get("Payload-Oxum"))
              .externalDescription(fileValues.get("External-Description"))
              .contactMail(fileValues.get("Contact-Email"))
              .build();
    } catch(NullPointerException e){
      throw new IngestProcessingException(
          "Failed to extract a required key from " + BagFilePaths.BAG_INFO_FILE_PATH.name + " to intern BagInfo class. Original error: " + e.getMessage(),
          e
      );
    }

    // validate sip.json mapping
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()){
      Validator validator = factory.getValidator();
      Set<ConstraintViolation<BagInfo>> violations = validator.validate(bagInfo);
      if(!violations.isEmpty()){
          throw new IngestProcessingException(
              "Failed to validate bag info file from " + BagFilePaths.BAG_INFO_FILE_PATH.name + ". Original error: " + violations
          );
      }
    }

    return  bagInfo;
  }

    /**
     * Maps the key value pairs in the bagit.txt file to a BagMeta object.
     * @param bagDirPath path to the bag directory.
     * @return A BagMeta object.
     * @throws IngestProcessingException If the bagit.txt file is missing or if a required key is missing / validation fails.
     */
  public static BagMeta readBagItTxtFile(Path bagDirPath) throws IngestProcessingException {
    String pathToBagMetaFile = bagDirPath.resolve(BagFilePaths.BAG_TXT_FILE_PATH.name).toString();
    Map<String, String> fileValues = readKeyValueTxtFile(pathToBagMetaFile);

    BagMeta bagMeta;

    try {
      bagMeta = BagMeta.builder()
              .bagItVersion(fileValues.get("BagIt-Version"))
              .tagFileCharacterEncoding(fileValues.get("Tag-File-Character-Encoding"))
              .build();
    } catch(NullPointerException e){
      throw new IngestProcessingException(
          "Failed to extract a required key from " + BagFilePaths.BAG_METADATA_DIR.name + " to intern BagMeta class. Original error: " + e.getMessage(),
          e
      );
    }

    // bagIt.txt mapping
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()){
      Validator validator = factory.getValidator();
      Set<ConstraintViolation<BagMeta>> violations = validator.validate(bagMeta);
      if(!violations.isEmpty()){
          throw new IngestProcessingException(
              "Failed to validate bag meta file from " + BagFilePaths.BAG_METADATA_DIR.name + ". Original error: " + violations
          );
      }
    }

    return  bagMeta;
  }

  /**
   * Maps the key value pairs in defined text file to a map.
   * @param filePath The path to the text file.
   * @return A map of the key value pairs in the text file.
   * @throws IngestProcessingException If the file is missing or if the file is malformed (e.g. expected line count not reached or if extracted map is empty).
   */
  static Map<String, String> readKeyValueTxtFile(String filePath) throws IngestProcessingException {
    return readKeyValueTxtFile(filePath, ":");
  }

  /**
   * Maps the key value pairs in defined text file to a map.
   * @param filePath The path to the text file.
   * @return A map of the key value pairs in the text file.
   * @throws IngestProcessingException If the file is missing or if the file is malformed (e.g. expected line count not reached or if extracted map is empty).
   */
  static Map<String, String> readKeyValueTxtFile(String filePath, String delimiter) throws IngestProcessingException {
    Map<String, String> map = new HashMap<>();
    try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
      AtomicInteger lineCount = new AtomicInteger();
      lines.filter(line -> line.contains(delimiter))
              .forEach(line -> {
                String[] keyValuePair = line.split(delimiter, 2);
                String key = keyValuePair[0];
                String value = keyValuePair[1];
                // remove possible ending and leading whitespaces
                key = key.trim();
                value = value.trim();

                if(map.containsKey(key)){
                  throw new IngestBagDuplicatedChecksumException(
                      "Encountered duplicate checksum " + key + " in file "+ filePath
                  );
                }

                map.put(key, value);
                lineCount.getAndIncrement();
              });

      if(lineCount.get() != map.size()){
        throw new IngestProcessingException(
            "Failed to map key value pairs in file "+ filePath + " to map. The number of lines containing the delimiter (" + delimiter + ") does not match the number of entries in the resulting map. Got line count: " + lineCount.get() + " and map size: " + map.size() + ". The map is: " + map
        );
      }

    } catch (IOException e) {
      throw new IngestProcessingException(
          "Failed to map key value pairs in file "+ filePath + " to map. Original error: " + e.getMessage(),
          e
      );
    }

    if(map.isEmpty()){
      throw new IngestProcessingException(
          "Failed to map key value pairs in file "+ filePath + " to map."
      );
    }

    return map;
  }

  /**
   * Maps the sip.json file to a BagSipJson object.
   * @param bagDirPath The path to the bag root directory.
   */
  public static BagSipJson readSipJson(Path bagDirPath) throws IngestProcessingException {

    Path pathToBagInfoFile = bagDirPath.resolve(BagFilePaths.BAG_SIP_JSON.name);

    byte[] jsonContent;
    try {
      jsonContent = Files.readAllBytes(pathToBagInfoFile);
    } catch (IOException e){
      throw new IngestProcessingException(
          "Failed to read out sip.json from " + BagFilePaths.BAG_SIP_JSON.name + ". Original error: " + e.getMessage(),
          e);
    }

    BagSipJson bagSipJson;
    try {
      // TODO every time new instantiation seems a lot of overhead
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      bagSipJson = new ObjectMapper().readValue(jsonContent, BagSipJson.class);
    } catch (IOException e){
      throw new IngestProcessingException(
          "Failed to map sip.json from " + BagFilePaths.BAG_SIP_JSON.name + " to BagitSipJson class. Original error:, " + e.getMessage(),
          e
      );
    }

    // check if dsids are unique
    Set<String> containedDsids = bagSipJson.getContentFiles().stream().map(BagSipJsonContentFile::getDsid).collect(Collectors.toSet());
    if(containedDsids.size() != bagSipJson.getContentFiles().size()){
      throw new IngestProcessingException(
          "Failed to validate sip.json from " + BagFilePaths.BAG_SIP_JSON.name + ". Duplicate dsids found in content files. SIPJson: " + bagSipJson
      );
    }
    // check if DC dsid is present - or more in future
    if(!containedDsids.contains(GAMSDsid.DC.getValue())){
      throw new IngestProcessingException(
          "Encountered invalid sip.json from " + BagFilePaths.BAG_SIP_JSON.name + ". There must be a " + GAMSDsid.DC.getValue() + " dsid present as contentFile in the sip.json. SIPJson: " + bagSipJson
      );
    }

    // validate sip.json mapping
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()){
      Validator validator = factory.getValidator();
      Set<ConstraintViolation<BagSipJson>> violations = validator.validate(bagSipJson);
        if(!violations.isEmpty()){
            throw new IngestProcessingException(
                "Failed to validate sip.json from " + BagFilePaths.BAG_SIP_JSON.name + ". Original error: " + violations
            );
        }
    }

    return bagSipJson;
   }

}
