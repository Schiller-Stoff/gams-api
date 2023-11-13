package org.zim.gamsapi.SubInfoPack;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.SubInfoPack.exceptions.SubInfoPackProcessingException;
import org.zim.gamsapi.SubInfoPack.interfaces.ISubInfoPackService;
import org.zim.gamsapi.SubInfoPack.utils.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubInfoPackService implements ISubInfoPackService {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;
  private final IProjectRepository projectRepository;

  @Override
  @Transactional
  public void ingest(SubInfoPack subInfoPack) {

    // 01. unzip bagitinfo to temp
    Path unzippedBag = unzipBagToTempDir(subInfoPack);
    BagItInfo bagItInfo = BagitUtils.mapBagItInfo(unzippedBag);

    log.info("****** Successfully extracted bagit-info.txt: {}", bagItInfo);

    // 02. build and save digital object from bag-info.txt
    DigitalObject digitalObject = DigitalObject.builder()
            .id(bagItInfo.getId())
            .project(Project.builder().projectAbbr(subInfoPack.getProjectAbbr()).build())
            .build();

    digitalObjectRepository.save(digitalObject);
    log.info("****** Successfully saved digital object: {}", digitalObject);

    // TODO loop through data directory and create datastreams accordingly

    // TODO at the end remove temp directory

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

    // first create random named temp directory
    Path tempBagDirPath;
    try {
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


}
