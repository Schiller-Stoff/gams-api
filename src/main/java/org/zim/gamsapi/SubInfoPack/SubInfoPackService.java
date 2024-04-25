package org.zim.gamsapi.SubInfoPack;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.Datastream.DatastreamBuilder;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DigitalObjectBuilder;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.MetadataBaseEntityBuilder;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.SubInfoPack.exceptions.SubInfoPackProcessingException;
import org.zim.gamsapi.SubInfoPack.interfaces.ISubInfoPackService;
import org.zim.gamsapi.SubInfoPack.utils.*;
import org.zim.gamsapi.SubInfoPack.utils.Bagit.BagitSipJson;
import org.zim.gamsapi.SubInfoPack.utils.Bagit.BagItDirectoryReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubInfoPackService implements ISubInfoPackService {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;

  @Override
  @Transactional
  public void ingest(SubInfoPack subInfoPack) {

    // 01. unzip bagitinfo to temp
    Path unzippedBag = unzipBagToTempDir(subInfoPack);

    try {
      BagitSipJson bagitSipJson = BagItDirectoryReader.extractSipJson(unzippedBag);
      log.error("****** Successfully extracted bagit sip.json: {}", bagitSipJson);

      String parentId = bagitSipJson.getParent();
      // if there are child objects -> save a reference

      // 02. build and save digital object from bag-info.txt
      DigitalObject digitalObject = new DigitalObjectBuilder()
          .id(bagitSipJson.getId())
          .project(Project.builder().projectAbbr(subInfoPack.getProjectAbbr()).build())
          .objectType(bagitSipJson.getObjectType())
          .parent(new DigitalObjectBuilder().id(parentId).build())
          .types(bagitSipJson.getTypes())
          .baseMetadata(new MetadataBaseEntityBuilder()
              .title(bagitSipJson.getTitle())
              .creator(bagitSipJson.getCreator())
              .description(bagitSipJson.getDescription())
              .publisher(bagitSipJson.getPublisher())
              .rights(bagitSipJson.getRights())
              .build())
          .build();

      digitalObjectRepository.save(digitalObject);
      log.info("****** Successfully saved digital object: {}", digitalObject);

      // 03. build and save datastreams from sip.json in the bagit payload
      bagitSipJson.getContentFiles().stream()
            .map(contentFile -> {
              byte[] datastreamContent;
              Path contentFilePath = Path.of(unzippedBag + File.separator + contentFile.getBagpath());
              try {
                datastreamContent = Files.readAllBytes(contentFilePath);
              } catch (IOException e) {
                String msg = String.format("Failed to read file %s for given subinfopack %s for object %s. Original error %s", contentFilePath, subInfoPack, digitalObject, e);
                log.error(msg);
                throw new SubInfoPackProcessingException(msg);
              }
              return new DatastreamBuilder()
                      .dsid(contentFile.getDsid())
                      .digitalObject(digitalObject)
                      .data(datastreamContent)
                      .mimeType(contentFile.getMimetype())
                      .size(contentFile.getSize())
                      .fileName(contentFilePath.getFileName().toString())
                      .baseMetadata(new MetadataBaseEntityBuilder()
                              .title(contentFile.getTitle())
                              .creator(contentFile.getCreator())
                              .description(contentFile.getDescription())
                              .publisher(contentFile.getPublisher())
                              .rights(contentFile.getRights())
                              .build())
                      .build();
            })
            .forEach(datastreamRepository::save);

    } catch (Exception e){
      // make sure that in any case the temp directory is deleted
      deleteDir(unzippedBag);
      throw e;
    }

  }

  /**
   * Adds element to
   * If given list is null creates a List and add the item -> returns list.
   * If given list exists -> copies list and adds given element -> returns list
   * @param nullableList given list that might be null
   * @param itemToAdd item to add to list.
   * @return created list with added item.
   */
  private List<String> addToNullableList(@Nullable List<String> nullableList, String itemToAdd){
    if((nullableList == null) || (nullableList.size() == 0)) {
      return new ArrayList<>(List.of(itemToAdd));
    } else {
      List<String> copiedList = new ArrayList<>(nullableList);
      copiedList.add(itemToAdd);
      return copiedList;
    }
  }

  /**
   * Unzips given submission information package to temporary directory.
   * @param subInfoPack submission information package to be processed.
   * @return path to temporary directory containing unzipped submission information package as bagit.
   * @throws SubInfoPackProcessingException if unzipping fails.
   */
  private Path unzipBagToTempDir(SubInfoPack subInfoPack) throws SubInfoPackProcessingException {

    // TODO should I move this logic to ZpiUtils? (and test there?) - because: 1. it is a utility method 2. would be easier to test

    // first create random named temp directory
    Path tempBagDirPath;
    try {
      // TODO think about this
      //tempBagDirPath = Files.createTempDirectory(subInfoPack.getProjectAbbr() + "_" + UUID.randomUUID().toString());
       tempBagDirPath = Files.createTempDirectory(subInfoPack.getProjectAbbr());
    } catch (IOException e){
      String msg = String.format("Failed to create root temporary directory for given subinfopack %s. Original error %s", subInfoPack, e);
      log.error(msg);
      throw new SubInfoPackProcessingException(msg);
    }

    // walk through zipped directory and create directories and files in temp directory
    ZipUtils.walkZippedDir(subInfoPack.getZippedFolder(), (zipEntry, byteArrayOutputStream) -> {
      Path tempFilePath = tempBagDirPath.resolve(zipEntry.getName());
      if(zipEntry.isDirectory()){
        try {
          Files.createDirectories(tempFilePath);
          log.info("Created temporary bag directory: {}", tempFilePath);
        } catch (IOException e) {
          String msg = String.format("Failed to create directory %s for given subinfopack %s. Original error %s", tempFilePath, subInfoPack, e);
          log.error(msg);
          throw new SubInfoPackProcessingException(msg);
        }
      } else {
        try {
          // zip might contain entries like /datastreams/derla.sty1 --> need to create /datastreams/ directory first
          ensureParentDir(tempFilePath);
          Files.createFile(tempFilePath);
          Files.write(tempFilePath, byteArrayOutputStream.toByteArray());
          log.info("Successfully wrote file {} to temporary bag directory: {}", zipEntry.getName(), tempFilePath);
        } catch (IOException e) {
          String msg = String.format("Failed to create file %s for given subinfopack %s. Original error %s", tempFilePath, subInfoPack, e);
          log.error(msg);
          throw new SubInfoPackProcessingException(msg);
        }
      }
    });

    return tempBagDirPath;
  }


  /**
   * Makes sure that all parent directories of the given path exist.
   * @param path path to check
   * @throws SubInfoPackProcessingException if missing parent directories cannot be created
   */
  private void ensureParentDir(Path path) throws SubInfoPackProcessingException {
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
        throw new SubInfoPackProcessingException(msg);
      }

    }
  }


  /**
   * Deletes given directory and all its subdirectories and files.
   * @param dirPath path to directory to be deleted.
   * @throws SubInfoPackProcessingException if deletion fails.
   */
  private void deleteDir(Path dirPath) throws SubInfoPackProcessingException {
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
      throw new SubInfoPackProcessingException(msg);
    }
  }

}
