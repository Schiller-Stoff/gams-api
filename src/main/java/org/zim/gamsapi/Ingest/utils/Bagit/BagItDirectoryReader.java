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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for BagIt related operations.
 */
@Slf4j
public class BagItDirectoryReader {

  // TODO maybe better to solve this with a constructor - bagitPath as parameter?
  // TODO want to test methods that get the extracted bagit as argument and not the path to the bagit?
  // (would be easier to test)

  // TODO atm e.g. bagSipJson is being validated BUT not the bagit as a whole -- would need to check certain file conventions etc.?
  // (if checksums are available etc)

  /**
   * Maps the key value pairs in the bag-info.txt file to a BagItInfo object.
   * @param bagItDirPath The path to the bagit directory.
   * @return A BagItInfo object.
   * @throws IngestProcessingException If the bag-info.txt file is missing or if a required key is missing.
   */
  public static BagItInfo extractBagItInfo(Path bagItDirPath) throws IngestProcessingException {

    // TODO do I really need to check the bag-info.txt file? (if it is missing, the whole bag is invalid)

    String pathToBagInfoFile = bagItDirPath.resolve(BagItFilePaths.BAG_INFO_FILE_PATH.name).toString();
    Map<String, String> fileValues = mapKeyValueTextFile(pathToBagInfoFile);

    try {
      //TODO validation of baginfo is missing!
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
      throw new IngestProcessingException(msg);
    }

  }



  /**
   * Maps the key value pairs in defined text file to a map.
   * @param filePath The path to the text file.
   * @return A map of the key value pairs in the text file.
   */
  private static Map<String, String> mapKeyValueTextFile(String filePath) throws IngestProcessingException {
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
      throw new IngestProcessingException(msg);
    }
    return map;
  }

  /**
   * Maps the sip.json file to a BagitSipJson object.
   * @param bagitDirPath The path to the bagit directory.
   */
  public static BagitSipJson extractAndValidateSipJson(Path bagitDirPath){

    Path pathToBagInfoFile = bagitDirPath.resolve(BagItFilePaths.BAG_SIP_JSON.name);

    byte[] jsonContent;
    try {
      jsonContent = Files.readAllBytes(pathToBagInfoFile);

    } catch (IOException e){
      String msg = String.format("Failed to read out sip.json from %s. Original error: %s", BagItFilePaths.BAG_SIP_JSON.name, e);
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

    BagitSipJson bagitSipJson;
    try {
      // TODO instantiation of own object mapper might be just a waste of resources. (inject object mapper instead)
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      bagitSipJson = new ObjectMapper().readValue(jsonContent, BagitSipJson.class);
    } catch (IOException e){
      String msg = String.format("Failed to map sip.json from %s to BagitSipJson class. Original error: %s", BagItFilePaths.BAG_SIP_JSON.name, e);
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

    // check if dsids are unique
    Set<String> containedDsids = bagitSipJson.getContentFiles().stream().map(BagitContentFile::getDsid).collect(Collectors.toSet());
    if(containedDsids.size() != bagitSipJson.getContentFiles().size()){
      String msg = String.format("Failed to validate sip.json from %s. Duplicate dsids found in content files. SIPJson: %s", BagItFilePaths.BAG_SIP_JSON.name, bagitSipJson);
      log.error(msg);
      throw new IngestProcessingException(msg);
    }
    // check if DC dsid is present - or more in future
    if(!containedDsids.contains(GAMSDsid.DC.getValue())){
      String msg = String.format("Encountered invalid sip.json from %s. There must be a %s dsid present as contentFile in the sip.json. %s",  BagItFilePaths.BAG_SIP_JSON.name, GAMSDsid.DC.getValue(), bagitSipJson);
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

    // TODO maybe inject instead of creating new validator factory every time?
    // validate sip.json mapping
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()){
      Validator validator = factory.getValidator();
      Set<ConstraintViolation<BagitSipJson>> violations = validator.validate(bagitSipJson);
        if(!violations.isEmpty()){
            String msg = String.format("Failed to validate sip.json from %s. Original error: %s", BagItFilePaths.BAG_SIP_JSON.name, violations);
            log.error(msg);
            throw new IngestProcessingException(msg);
        }
    }

    return bagitSipJson;
   }

}
