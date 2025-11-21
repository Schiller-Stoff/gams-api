package org.ddh.gamsapi.application.Ingest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Ingest.exceptions.*;
import org.ddh.gamsapi.application.Ingest.interfaces.IIngestService;
import org.ddh.gamsapi.application.Ingest.utils.Bagit.Bag;
import org.ddh.gamsapi.application.Ingest.utils.Bagit.BagData;
import org.ddh.gamsapi.application.Ingest.utils.Bagit.BagInfo;
import org.ddh.gamsapi.application.Ingest.utils.Bagit.BagMeta;
import org.ddh.gamsapi.application.Ingest.utils.ZipUtils;
import org.ddh.gamsapi.application.Integration.Common.utils.XMLUtils;
import org.ddh.gamsapi.domain.Datastream.Datastream;
import org.ddh.gamsapi.domain.Datastream.utils.GAMSDsid;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObjectCreatedEvent;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.DublinCoreEntry;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.ddh.gamsapi.domain.DigitalObject.SubmissionRecord.ISubmissionRecordRepository;
import org.ddh.gamsapi.domain.DigitalObject.SubmissionRecord.SubmissionRecord;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectNotFoundException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
  public void ingest(String projectAbbr, InputStream bagZipStream) {

    var foundProject = projectRepository.findById(projectAbbr)
        .orElseThrow(() -> new ProjectNotFoundException(
            String.format("Project %s does not exist", projectAbbr)
        ));

    // Unzip DIRECTLY from stream to temp directory (no byte[] intermediate)
    Path bagDirPath;
    try {
      bagDirPath = ZipUtils.unzipStreamToTempDir(bagZipStream);
    } catch (IngestProcessingException e) {
      String msg = String.format("Failed to unzip bag for project %s: %s",
          projectAbbr, e.getMessage());
      log.error(msg, e);
      throw new IngestProcessingException(msg);
    }

    try {
      // 02. Bag processing
      Bag bag = new Bag(bagDirPath);

      log.debug("Successfully extracted bag: {}", bag.getBAG_DIR_PATH());
      if(!bag.getBagData().getProject().equals(projectAbbr)){
        String msg = String.format("The project abbreviation of the ingest %s does not match the project %s in the bag sip.json. (Make sure that your bags describe the same project as your ingest request). Aborting ingest operation. Happened at BagSipJson: %s", projectAbbr, bag.getBagData().getProject(), bag.getBagData());
        throw new IngestAgainstDifferentProjectException(msg);
      }

      // 03. build and save digital object from bag-info.txt
      DigitalObject digitalObject = conversionService.convert(bag.getBagData(), DigitalObject.class);
      if(digitalObject == null){
        String msg = String.format("Digital object is unexpectedly null. Failed to convert bag data %s to digital object for given ingest against project %s", bag.getBagData(), projectAbbr);
        throw new IngestTypeConversionException(msg);
      }

      // abort ingest if digital object already exists
      if(digitalObjectRepository.existsById(digitalObject.getId())){
        String msg = String.format("Cannot ingest object with id %s. Digital object already exists and must be deleted before another ingest process. Ingest against project: %s", digitalObject.getId(), projectAbbr);
        throw new IngestObjectAlreadyExistsException(msg);
      }

      final DigitalObject savedObject = digitalObjectRepository.save(digitalObject);
      log.debug("Successfully saved digital object: {} for project {}", digitalObject, projectAbbr);

      // logic to save the related BagEntities
      var bagEntity = SubmissionRecord.builder()
              .digitalObject(savedObject)
              .createdBy(bag.getBagData().getCreatedBy())
              .source(bag.getBagData().getSource())
              .schema(bag.getBagData().getSchema())
              .contactMail(bag.getBagInfo().getContactMail())
              .baggingDate(bag.getBagInfo().getDate())
              .externalDescription(bag.getBagInfo().getExternalDescription())
              .payloadOxum(bag.getBagInfo().getPayloadOxum())
              .bagVersion(bag.getBagMeta().getBagItVersion())
              .tagFileCharacterEncoding(bag.getBagMeta().getTagFileCharacterEncoding())
              .build();

      bagEntityRepository.save(bagEntity);
      log.debug("Successfully saved bag entity: {} for project {}", bagEntity, projectAbbr);

      // 04. build and save datastreams from the bag data
      bag.getBagData().getContentFiles()
            .forEach(contentFile -> {
              Datastream datastream = conversionService.convert(contentFile, Datastream.class);
              if(datastream == null){
                String msg = String.format("Datastream is unexpectedly null. Failed to convert contentFile %s to datastream for project %s for object %s", contentFile, projectAbbr, digitalObject);
                throw new IngestTypeConversionException(msg);
              }

              // set datastream to point to the saved digital object
              datastream.setDigitalObject(savedObject);

              // saving the datastream content to the filesystem via streaming
              Path contentFilePath = Path.of(bagDirPath + File.separator + contentFile.getBagpath());
              try (InputStream inputStream = Files.newInputStream(contentFilePath)) {
                datastreamContentRepository.save(inputStream, datastream.deriveDatastreamId());
              } catch (IOException e) {
                String msg = "Failed to save datastream content from " +
                    contentFilePath + " for object " + digitalObject.getId();
                log.error(msg, e);
                throw new IngestProcessingException(msg);
              }

              // save datastream to database
              // make sure that the files are being deleted in any case (if database error occurs).
              try {
                datastreamRepository.save(datastream);
                log.info("Successfully saved datastream {}", datastream);

                // also save dublin core metadata
                // TODO dublin core processing - move to integration layer somewhere - this is shaky in my oppinion
                if(contentFile.getDsid().equals(GAMSDsid.DC.getValue())){
                  // read only the datastream content for dublin core
                  byte[] dublinCoreContent;
                  Path dcFilePath = Path.of(bagDirPath + File.separator + contentFile.getBagpath());
                  try {
                    dublinCoreContent = Files.readAllBytes(dcFilePath);
                  } catch (IOException e) {
                    String msg = String.format("Failed to read file %s for given project %s for object %s for datastream %s. Original error %s", contentFilePath, projectAbbr, digitalObject, datastream, e);
                    throw new IngestProcessingException(msg);
                  }

                  Document dublinCore = XMLUtils.parseXml(dublinCoreContent);
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

    } finally {
      // cleanup temp directory in any case
      try {
        ZipUtils.deleteDir(bagDirPath);
      } catch (Exception e ){
        log.error(e.getMessage(), e);
      }
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
        () -> new DigitalObjectNotFoundException(
            "Failed to export digital object as bag. Digital object does not exist:  " + objectId
        )
    );

    var ingestRecord = bagEntityRepository.findById(objectId).orElseThrow(
        () -> {
          // TODO this is a server error - use different exception?
          return new DigitalObjectNotFoundException(
              "Cannot export digital object as bag: " + objectId
          );
        }
    );

    var datastreams = datastreamRepository.findAllByDigitalObject(digitalObject);

    if(datastreams.isEmpty()){
      throw new ExportUnexpectedObjectStateException(
          "Cannot export digital object as bag - no datastreams found for object: " + objectId
      );
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
