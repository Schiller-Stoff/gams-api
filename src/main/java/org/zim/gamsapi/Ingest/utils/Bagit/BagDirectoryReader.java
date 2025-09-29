package org.zim.gamsapi.Ingest.utils.Bagit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.zim.gamsapi.Datastream.GAMSDsid;
import org.zim.gamsapi.Ingest.exceptions.IngestProcessingException;
import org.zim.gamsapi.Ingest.utils.Bagit.mapping.BagSipJsonContentFile;
import org.zim.gamsapi.Ingest.utils.Bagit.mapping.BagSipJson;

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

  // TODO maybe better to solve this with a constructor - bagitPath as parameter?
  // TODO want to test methods that get the extracted bagit as argument and not the path to the bagit?
  // (would be easier to test)

  // TODO atm e.g. bagSipJson is being validated BUT not the bagit as a whole -- would need to check certain file conventions etc.?
  // (if checksums are available etc)

  /**
   * TODO Jdoc
   * TODO test
   * @param bagItDirPath
   * @return
   * @throws IngestProcessingException
   */
  public static Map<String, String> extractBagPathSha512Map(Path bagItDirPath) throws IngestProcessingException {
    String pathToManifestFile = bagItDirPath.resolve(BagFilePaths.MANIFEST_SHA512_FILE_PATH.name).toString();
    // return a map of dsid to sha512 checksum

    var checksumPathsMap = mapKeyValueTextFile(pathToManifestFile, " ");
    var dsidChecksumMap = new HashMap<String, String>();

    checksumPathsMap.forEach(
        (sha512Checksum, bagPath) -> {
           if(dsidChecksumMap.containsValue(sha512Checksum)){
            String msg = String.format("Encountered duplicate checksum %s in sha512 manifest file %s.", sha512Checksum, BagFilePaths.MANIFEST_SHA512_FILE_PATH.name);
            log.error(msg);
            throw new IngestProcessingException(msg);
           }
           if(sha512Checksum.length() != 128) {
            String msg = String.format("Encountered invalid sha512 checksum for dsid %s in sha512 manifest file %s. Checksum must be 128 characters long.", sha512Checksum, BagFilePaths.MANIFEST_SHA512_FILE_PATH.name);
            log.error(msg);
            throw new IngestProcessingException(msg);
           }

          if(bagPath == null){
            String msg = String.format("Encountered null bag path for checksum %s in sha512 manifest file %s.", sha512Checksum, BagFilePaths.MANIFEST_MD5_FILE_PATH.name);
            log.error(msg);
            // TODO different exception?
            throw new IngestProcessingException(msg);
          }

          if(bagPath.isBlank()){
            String msg = String.format("Encountered empty bag path for checksum %s in sha512 manifest file %s.", sha512Checksum, BagFilePaths.MANIFEST_MD5_FILE_PATH.name);
            log.error(msg);
            // TODO different exception?
            throw new IngestProcessingException(msg);
          }

           dsidChecksumMap.put(bagPath, sha512Checksum);
        }
    );

    return dsidChecksumMap;
  }

  /**
   * TODO jdoc
   * TODO test
   * TODO somewaht duplicated with sha-512 logic
   * @param bagItDirPath
   * @return
   * @throws IngestProcessingException
   */
  public static Map<String,String> extractBagPathMd5Map(Path bagItDirPath) throws IngestProcessingException {
    String pathToManifestFile = bagItDirPath.resolve(BagFilePaths.MANIFEST_MD5_FILE_PATH.name).toString();

    // return a map of dsid to md5 checksum
    var dsidChecksumMap = new HashMap<String, String>();
    var checksumPathsMap = mapKeyValueTextFile(pathToManifestFile, " ");

    checksumPathsMap.forEach(
        (checksum, bagPath) -> {
          if(dsidChecksumMap.containsKey(checksum)){
            String msg = String.format("Encountered duplicate checksum %s in md5 manifest file %s.", checksum, BagFilePaths.MANIFEST_MD5_FILE_PATH.name);
            log.error(msg);
            throw new IngestProcessingException(msg);
          }
          if(checksum.length() != 32) {
            String msg = String.format("Encountered invalid md5 checksum %s in md5 manifest file %s. Checksum must be 32 characters long.", checksum, BagFilePaths.MANIFEST_SHA512_FILE_PATH.name);
            log.error(msg);
            throw new IngestProcessingException(msg);
          }

          if(bagPath == null){
            String msg = String.format("Encountered null bag path for checksum %s in md5 manifest file %s.", checksum, BagFilePaths.MANIFEST_MD5_FILE_PATH.name);
            log.error(msg);
            // TODO different exception?
            throw new IngestProcessingException(msg);
          }

          if(bagPath.isBlank()){
            String msg = String.format("Encountered empty bag path for checksum %s in md5 manifest file %s.", checksum, BagFilePaths.MANIFEST_MD5_FILE_PATH.name);
            log.error(msg);
            // TODO different exception?
            throw new IngestProcessingException(msg);
          }

          dsidChecksumMap.put(bagPath, checksum);
        }
    );
    return dsidChecksumMap;
  }

  /**
   * Maps the key value pairs in the bag-info.txt file to a BagItInfo object.
   * @param bagItDirPath The path to the bagit directory.
   * @return A BagItInfo object.
   * @throws IngestProcessingException If the bag-info.txt file is missing or if a required key is missing.
   */
  public static BagInfo readBagInfoFile(Path bagItDirPath) throws IngestProcessingException {

    // TODO do I really need to check the bag-info.txt file? (if it is missing, the whole bag is invalid)
    // TODO solve todos

    String pathToBagInfoFile = bagItDirPath.resolve(BagFilePaths.BAG_INFO_FILE_PATH.name).toString();
    Map<String, String> fileValues = mapKeyValueTextFile(pathToBagInfoFile);

    try {
      //TODO validation of baginfo is missing!
      return  BagInfo.builder()
              .date(fileValues.get("Bagging-Date"))
              .time(fileValues.get("Bagging-Time"))
              .payloadOxum(fileValues.get("Payload-Oxum"))
              .externalDescription(fileValues.get("External-Description"))
              .contactMail(fileValues.get("Contact-Email"))
              .build();
    } catch(NullPointerException e){
      String msg = String.format("Failed to extract a required key from %s to intern BagItInfo class. Original error: %s", BagFilePaths.BAG_INFO_FILE_PATH.name, e);
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

  }

  /**
   * TODO jdoc
   * @param filePath
   * @return
   * @throws IngestProcessingException
   */
  private static Map<String, String> mapKeyValueTextFile(String filePath) throws IngestProcessingException {
    return mapKeyValueTextFile(filePath, ":");
  }

  /**
   * Maps the key value pairs in defined text file to a map.
   * @param filePath The path to the text file.
   * @return A map of the key value pairs in the text file.
   */
  private static Map<String, String> mapKeyValueTextFile(String filePath, String delimiter) throws IngestProcessingException {
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
                map.put(key, value);
                lineCount.getAndIncrement();
              });

      if(lineCount.get() != map.size()){
        String msg = String.format("Failed to map key value pairs in file %s to map. The number of lines containing the delimiter (%s) does not match the number of entries in the resulting map.", filePath, delimiter);
        log.error(msg);
        // TODO better exception e.g. IO exception
        throw new IngestProcessingException(msg);
      }

    } catch (IOException e) {
      String msg = String.format("Failed to map key value pairs in file %s to map. Original error: %s", filePath, e);
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

    if(map.isEmpty()){
      String msg = String.format("Failed to map key value pairs in file %s to map. The resulting map is empty.", filePath);
      log.error(msg);
      // TODO use different - best a checked exception? (IOException e.g.?)
      throw new IngestProcessingException(msg);
    }

    return map;
  }

  /**
   * Maps the sip.json file to a BagSipJson object.
   * @param bagDirPath The path to the bag root directory.
   */
  public static BagSipJson extractAndValidateSipJson(Path bagDirPath) throws IngestProcessingException {

    Path pathToBagInfoFile = bagDirPath.resolve(BagFilePaths.BAG_SIP_JSON.name);

    byte[] jsonContent;
    try {
      jsonContent = Files.readAllBytes(pathToBagInfoFile);
    } catch (IOException e){
      String msg = String.format("Failed to read out sip.json from %s. Original error: %s", BagFilePaths.BAG_SIP_JSON.name, e);
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

    BagSipJson bagSipJson;
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      bagSipJson = new ObjectMapper().readValue(jsonContent, BagSipJson.class);
    } catch (IOException e){
      String msg = String.format("Failed to map sip.json from %s to BagitSipJson class. Original error: %s", BagFilePaths.BAG_SIP_JSON.name, e);
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

    // check if dsids are unique
    Set<String> containedDsids = bagSipJson.getContentFiles().stream().map(BagSipJsonContentFile::getDsid).collect(Collectors.toSet());
    if(containedDsids.size() != bagSipJson.getContentFiles().size()){
      String msg = String.format("Failed to validate sip.json from %s. Duplicate dsids found in content files. SIPJson: %s", BagFilePaths.BAG_SIP_JSON.name, bagSipJson);
      log.error(msg);
      throw new IngestProcessingException(msg);
    }
    // check if DC dsid is present - or more in future
    if(!containedDsids.contains(GAMSDsid.DC.getValue())){
      String msg = String.format("Encountered invalid sip.json from %s. There must be a %s dsid present as contentFile in the sip.json. %s",  BagFilePaths.BAG_SIP_JSON.name, GAMSDsid.DC.getValue(), bagSipJson);
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

    // validate sip.json mapping
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()){
      Validator validator = factory.getValidator();
      Set<ConstraintViolation<BagSipJson>> violations = validator.validate(bagSipJson);
        if(!violations.isEmpty()){
            String msg = String.format("Failed to validate sip.json from %s. Original error: %s", BagFilePaths.BAG_SIP_JSON.name, violations);
            log.error(msg);
            throw new IngestProcessingException(msg);
        }
    }

    return bagSipJson;
   }

}
