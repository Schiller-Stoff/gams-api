package org.zim.gamsapi.Ingest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.Datastream.interfaces.IFileSystemRepository;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.Ingest.exceptions.IngestTypeConversionException;
import org.zim.gamsapi.Ingest.exceptions.IngestProcessingException;
import org.zim.gamsapi.Ingest.interfaces.IIngestService;
import org.zim.gamsapi.Ingest.utils.*;
import org.zim.gamsapi.Ingest.utils.Bagit.BagitSipJson;
import org.zim.gamsapi.Ingest.utils.Bagit.BagItDirectoryReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
@RequiredArgsConstructor
public class IngestService implements IIngestService {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;
  private final ConversionService conversionService;
  private final IFileSystemRepository fileSystemRepository;

  @Override
  @Transactional
  public void ingest(Ingest ingest) {

    // 01. unzip bagitinfo to temp
    Path bagDirPath;
    try {
      bagDirPath = ZipUtils.unzipToTempDir(ingest.getZippedBagItFolder());
    } catch (IngestProcessingException e){
      // provide more context information for the logging and user.
      String msg = String.format("Failed to ingest given ingest operation %s. Original error: %s", ingest, e);
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

    try {
      BagitSipJson bagitSipJson = BagItDirectoryReader.extractAndValidateSipJson(bagDirPath);
      log.info("Successfully extracted bagit sip.json: {}", bagitSipJson);

      // 02. build and save digital object from bag-info.txt
      DigitalObject digitalObject = conversionService.convert(bagitSipJson, DigitalObject.class);
      if(digitalObject == null){
        String msg = String.format("Digital object is unexpectedly null. Failed to convert bagitSipJson %s to digital object for given ingest %s", bagitSipJson, ingest);
        log.error(msg);
        throw new IngestTypeConversionException(msg);
      }
      digitalObjectRepository.save(digitalObject);
      log.info("****** Successfully saved digital object: {} for ingest operation {}", digitalObject, ingest);

      // 03. build and save datastreams from sip.json in the bagit payload
      bagitSipJson.getContentFiles().stream()
            .map(contentFile -> {
              Datastream datastream = conversionService.convert(contentFile, Datastream.class);
              if(datastream == null){
                String msg = String.format("Datastream is unexpectedly null. Failed to convert contentFile %s to datastream for given ingest %s for object %s", contentFile, ingest, digitalObject);
                log.error(msg);
                throw new IngestTypeConversionException(msg);
              }

              // things need to be set aside from conversion.
              byte[] datastreamContent;
              Path contentFilePath = Path.of(bagDirPath + File.separator + contentFile.getBagpath());
              try {
                datastreamContent = Files.readAllBytes(contentFilePath);
              } catch (IOException e) {
                String msg = String.format("Failed to read file %s for given ingest %s for object %s for datastream %s. Original error %s", contentFilePath, ingest, digitalObject, datastream, e);
                log.error(msg);
                throw new IngestProcessingException(msg);
              }

              datastream.setDigitalObject(digitalObject);
              datastream.setData(datastreamContent);
              datastream.setFileName(contentFilePath.getFileName().toString());
              return datastream;
            })
            .forEach( datastream -> {
              // TODO test this procedure?
              // calculating sha3-256 hash from datastream -id
              String fileNameHashed = datastream.deriveDatastreamId().toString();
              fileSystemRepository.save(datastream.getData(), fileNameHashed);
              // save datastream to database
              // make sure that the files are being deleted in any case (if database error occurs).
              try {
                datastreamRepository.save(datastream);
              } catch (Exception e){
                // make sure that in any case the file on the filesystem is being deleted
                if(fileSystemRepository.exists(fileNameHashed)){
                  String msg = String.format("Failed to save datastream %s. Deleting correspondent file from filesystem %s", datastream, fileNameHashed);
                  log.error(msg);
                  fileSystemRepository.delete(fileNameHashed);
                };
                throw e;
              }
            });
    } catch (Exception e){
      // make sure that in any case the temp directory is deleted
      ZipUtils.deleteDir(bagDirPath);
      throw e;
    }

  }

}
