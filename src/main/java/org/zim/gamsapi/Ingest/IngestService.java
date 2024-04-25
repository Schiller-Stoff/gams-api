package org.zim.gamsapi.Ingest;

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
import org.zim.gamsapi.Ingest.exceptions.IngestProcessingException;
import org.zim.gamsapi.Ingest.interfaces.IIngestService;
import org.zim.gamsapi.Ingest.utils.*;
import org.zim.gamsapi.Ingest.utils.Bagit.BagitSipJson;
import org.zim.gamsapi.Ingest.utils.Bagit.BagItDirectoryReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class IngestService implements IIngestService {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;

  @Override
  @Transactional
  public void ingest(Ingest ingest) {

    // 01. unzip bagitinfo to temp
    Path bagDirPath;
    try {
      bagDirPath = ZipUtils.unzipToTempDir(ingest.getZippedBagItFolder());
    } catch (IngestProcessingException e){
      // provide more context information for the logging and user.
      String msg = String.format("Failed to ingest given ingest %s. Original error: %s", ingest, e);
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

    try {
      BagitSipJson bagitSipJson = BagItDirectoryReader.extractSipJson(bagDirPath);
      log.info("****** Successfully extracted bagit sip.json: {}", bagitSipJson);

      String parentId = bagitSipJson.getParent();
      // if there are child objects -> save a reference

      // 02. build and save digital object from bag-info.txt
      // TODO build object in seperate method - save can stay here?
      // TODO think about: looks like a conversion method?
      DigitalObject digitalObject = new DigitalObjectBuilder()
          .id(bagitSipJson.getId())
          .project(Project.builder().projectAbbr(ingest.getProjectAbbr()).build())
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
      // TODO build datastream in sepearate method - save can stay here?
      bagitSipJson.getContentFiles().stream()
            .map(contentFile -> {
              byte[] datastreamContent;
              Path contentFilePath = Path.of(bagDirPath + File.separator + contentFile.getBagpath());
              try {
                datastreamContent = Files.readAllBytes(contentFilePath);
              } catch (IOException e) {
                String msg = String.format("Failed to read file %s for given ingest %s for object %s. Original error %s", contentFilePath, ingest, digitalObject, e);
                log.error(msg);
                throw new IngestProcessingException(msg);
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
      ZipUtils.deleteDir(bagDirPath);
      throw e;
    }

  }

}
