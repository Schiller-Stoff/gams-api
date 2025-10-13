package org.zim.gamsapi.application.Ingest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.zim.gamsapi.application.Ingest.exceptions.*;
import org.zim.gamsapi.domain.Datastream.Datastream;
import org.zim.gamsapi.domain.Datastream.utils.GAMSDsid;
import org.zim.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.zim.gamsapi.domain.DigitalObject.DigitalObject;
import org.zim.gamsapi.domain.DigitalObject.DigitalObjectCreatedEvent;
import org.zim.gamsapi.domain.DigitalObject.DublinCoreEntry.DublinCoreEntry;
import org.zim.gamsapi.domain.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.zim.gamsapi.domain.DigitalObject.SubmissionRecord.SubmissionRecord;
import org.zim.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.zim.gamsapi.domain.DigitalObject.SubmissionRecord.ISubmissionRecordRepository;
import org.zim.gamsapi.application.Ingest.interfaces.IIngestService;
import org.zim.gamsapi.application.Ingest.utils.Bagit.Bag;
import org.zim.gamsapi.application.Ingest.utils.Bagit.BagData;
import org.zim.gamsapi.application.Ingest.utils.Bagit.BagInfo;
import org.zim.gamsapi.application.Ingest.utils.Bagit.BagMeta;
import org.zim.gamsapi.application.Ingest.utils.ZipUtils;
import org.zim.gamsapi.application.Integration.Common.utils.XMLUtils;
import org.zim.gamsapi.domain.Project.exceptions.ProjectNotFoundException;
import org.zim.gamsapi.domain.Project.interfaces.IProjectRepository;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

@Service
@Slf4j
@RequiredArgsConstructor
public class IngestService implements IIngestService {

  private final IProjectRepository projectRepository;
  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;
  private final ConversionService conversionService;
  private final IDatastreamContentRepository datastreamContentRepository;
  private final IDublinCoreEntryRepository dublinCoreElementRepository;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final ISubmissionRecordRepository bagEntityRepository;
  private final BuildProperties buildProperties;

  @Override
  @Transactional(rollbackFor = {
      // any exception will trigger a rollback
      Exception.class}
  )
  public void ingest(Ingest ingest) {

    var foundProject = projectRepository.findById(ingest.getProjectAbbr()).orElseThrow(
        () -> {
          String msg = String.format("Project defined for the ingest operation %s does not exist (defined in given request url - bag's sip.json was not analyzed). Denying ingest operation for ingest %s", ingest.getProjectAbbr(), ingest);
          log.warn(msg);
          return new ProjectNotFoundException(msg);
        }
    );

    // 01. unzip bag to temp
    Path bagDirPath;
    try {
      bagDirPath = ZipUtils.unzipToTempDir(ingest.getZippedBagItFolder());
    } catch (IngestProcessingException e){
      String msg = String.format("Failed to ingest given ingest operation %s. Original error: %s", ingest, e);
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

    try {
      // 02. Bag processing
      Bag bag = new Bag(bagDirPath);

      log.info("Successfully extracted bag: {}", bag.getBAG_DIR_PATH());
      if(!bag.getBagData().getProject().equals(ingest.getProjectAbbr())){
        String msg = String.format("The project abbreviation of the ingest %s does not match the project %s in the bag sip.json. (Make sure that your bags describe the same project as your ingest request). Aborting ingest operation %s. Happened at BagSipJson: %s", ingest.getProjectAbbr(), bag.getBagData().getProject(), ingest, bag.getBagData());
        log.error(msg);
        throw new IngestAgainstDifferentProjectException(msg);
      }

      // 03. build and save digital object from bag-info.txt
      DigitalObject digitalObject = conversionService.convert(bag.getBagData(), DigitalObject.class);
      if(digitalObject == null){
        String msg = String.format("Digital object is unexpectedly null. Failed to convert bag data %s to digital object for given ingest %s", bag.getBagData(), ingest);
        log.error(msg);
        throw new IngestTypeConversionException(msg);
      }

      // abort ingest if digital object already exists
      if(digitalObjectRepository.existsById(digitalObject.getId())){
        String msg = String.format("Cannot ingest object with id %s. Digital object already exists and must be deleted before another ingest process. Ingest metadata: %s", digitalObject.getId(), ingest);
        log.error(msg);
        throw new IngestObjectAlreadyExistsException(msg);
      }

      final DigitalObject savedObject = digitalObjectRepository.save(digitalObject);
      log.info("****** Successfully saved digital object: {} for ingest operation {}", digitalObject, ingest);

      // logic to save the related BagEntities
      var bagEntity = SubmissionRecord.builder()
              .digitalObject(savedObject)
              .createdBy(bag.getBagData().getCreatedBy())
              .source(bag.getBagData().getSource())
              .schema(bag.getBagData().getSchema())
              .contactMail(bag.getBagInfo().getContactMail())
              .baggingTimeStamp(bag.getBagInfo().getBaggingTimeStamp())
              .externalDescription(bag.getBagInfo().getExternalDescription())
              .payloadOxum(bag.getBagInfo().getPayloadOxum())
              .bagVersion(bag.getBagMeta().getBagItVersion())
              .tagFileCharacterEncoding(bag.getBagMeta().getTagFileCharacterEncoding())
              .build();

      bagEntityRepository.save(bagEntity);
      log.info("****** Successfully saved bag entity: {} for ingest operation {}", bagEntity, ingest);

      // 04. build and save datastreams from the bag data
      bag.getBagData().getContentFiles()
            .forEach(contentFile -> {
              Datastream datastream = conversionService.convert(contentFile, Datastream.class);
              if(datastream == null){
                String msg = String.format("Datastream is unexpectedly null. Failed to convert contentFile %s to datastream for given ingest %s for object %s", contentFile, ingest, digitalObject);
                log.error(msg);
                throw new IngestTypeConversionException(msg);
              }

              // things need to be set aside from conversion.
              // TODO usage of byte[] looks weird - because of streaming - maybe use inputstream?
              byte[] datastreamContent;
              Path contentFilePath = Path.of(bagDirPath + File.separator + contentFile.getBagpath());
              try {
                datastreamContent = Files.readAllBytes(contentFilePath);
              } catch (IOException e) {
                String msg = String.format("Failed to read file %s for given ingest %s for object %s for datastream %s. Original error %s", contentFilePath, ingest, digitalObject, datastream, e);
                log.error(msg);
                throw new IngestProcessingException(msg);
              }

              datastream.setDigitalObject(savedObject);

              // saving the datastream content to the filesystem
              datastreamContentRepository.save(datastreamContent, datastream.deriveDatastreamId());
              // save datastream to database
              // make sure that the files are being deleted in any case (if database error occurs).
              try {
                datastreamRepository.save(datastream);
                log.info("Successfully saved datastream {}", datastream);

                // also save dublin core metadata
                if(contentFile.getDsid().equals(GAMSDsid.DC.getValue())){
                  Document dublinCore = XMLUtils.parseXml(datastreamContent);
                  XMLUtils
                      .extractDCElements(dublinCore)
                      .forEach(dcElement -> {
                        DublinCoreEntry dublinCoreEntry = DublinCoreEntry
                            .builder()
                            .name(dcElement.getName())
                            .value(dcElement.getValue())
                            .language(dcElement.getLanguage())
                            .digitalObject(savedObject)
                            .build();
                        dublinCoreElementRepository.save(dublinCoreEntry);
                        log.info("Successfully saved dublinCoreEntry: {}", dublinCoreEntry);
                      });

                }

              } catch (Exception e){
                // make sure that in any case the file on the filesystem is being deleted
                if(datastreamContentRepository.exists(datastream.deriveDatastreamId())){
                  String msg = String.format("Failed to save datastream %s. For datastream file with name %s", datastream, datastream.deriveDatastreamId());
                  log.error(msg);
                  datastreamContentRepository.delete(datastream.deriveDatastreamId());
                }
                throw e;
              }
            });

      // tracks modification date of the content
      foundProject.setContentLastModified(new Date());
      applicationEventPublisher.publishEvent(
          new DigitalObjectCreatedEvent(this, savedObject)
      );

    } catch (Exception e){
      // make sure that in any case the temp directory is deleted
      ZipUtils.deleteDir(bagDirPath);
      throw e;
    }

  }

  /**
   * Export a digital object as a zipped BagIt package and write it to the provided OutputStream.
   * @param objectId the id of the digital object to export
   * @param outputStream the OutputStream to write the zipped BagIt package to
   */
  @Transactional
  public void exportAsBag(String objectId, OutputStream outputStream) {

    // 01. fetch data from database
    var digitalObject = digitalObjectRepository.findById(objectId).orElseThrow(
        () -> {
          String msg = String.format("Digital object with id %s does not exist. Cannot export non-existing object.", objectId);
          log.error(msg);
          return new DigitalObjectNotFoundException(msg);
        }
    );

    var ingestRecord = bagEntityRepository.findById(objectId).orElseThrow(
        () -> {
          String msg = String.format("Ingest record for digital object with id %s does not exist. Cannot export non-existing object.", objectId);
          log.error(msg);
          return new DigitalObjectNotFoundException(msg);
        }
    );

    var datastreams = datastreamRepository.findAllByDigitalObject(digitalObject);

    if(datastreams.isEmpty()){
      String msg = String.format("Digital object with id %s has no datastreams. Cannot export object without datastreams.", objectId);
      log.error(msg);
      throw new ExportUnexpectedObjectStateException(msg);
    }

    // 02. Map data to bag entities
    BagData bagData = BagData.from(digitalObject, datastreams, ingestRecord);
    BagMeta bagMeta =  BagMeta.from(ingestRecord);
    BagInfo bagInfo = BagInfo.from(ingestRecord);

    // create bag from database entities
    Bag bag = new Bag(bagInfo, bagMeta, bagData);

    // sert bag data entry to indicate that the bag was created by the gams-api
    String createdBy = String.format("%s %s",buildProperties.getName(), buildProperties.getVersion());
    bag.getBagData().setCreatedBy(createdBy);

    // 03. write bag
    bag.writeAsZipToStream(outputStream, datastreamContentRepository);


  }

}
