package org.ddh.gamsapi.application.Ingest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Ingest.exceptions.*;
import org.ddh.gamsapi.application.Ingest.interfaces.IIngestService;
import org.ddh.gamsapi.application.Ingest.utils.Bagit.*;
import org.ddh.gamsapi.application.Ingest.utils.ZipUtils;
import org.ddh.gamsapi.application.Integration.Common.utils.XMLUtils;
import org.ddh.gamsapi.domain.Datastream.Datastream;
import org.ddh.gamsapi.domain.Datastream.DatastreamId;
import org.ddh.gamsapi.domain.Datastream.utils.GAMSDsid;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.DublinCoreEntry;
import org.ddh.gamsapi.domain.DigitalObject.SubmissionRecord.ISubmissionRecordRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectNotFoundException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class IngestService implements IIngestService {

  private final IProjectRepository projectRepository;
  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;
  private final ConversionService conversionService;
  private final IDatastreamContentRepository datastreamContentRepository;
  private final ISubmissionRecordRepository submissionRecordRepository;
  private final BuildProperties buildProperties;

  // the persistence logic (@transactional) is in a separate service (called here) to ensure isolation
  // of heavy IO and database operations afterward (reason: performance - depletion of database connections).
  // self injecting to use a separate service method won't work for native builds!
  private final IngestPersistenceService ingestPersistenceService;

  public void ingest(String projectAbbr, InputStream bagZipStream) {

    // PHASE 0: check if project exists
    if (!projectRepository.existsById(projectAbbr)) {
      throw new ProjectNotFoundException(
          "Project does not exist: " + projectAbbr
      );
    }

    // preparation
    Path bagDirPath = null;
    List<DatastreamId> writtenFiles = new ArrayList<>(); // Track for rollback

    try {
      // --- PHASE 1: PREPARATION (No DB Connection) ---

      // 1. Unzip + parse bag
      bagDirPath = ZipUtils.unzipStreamToTempDir(bagZipStream);
      Bag bag = new Bag(bagDirPath);

      // 2. Logic Validation
      if (!bag.getBagData().getProject().equals(projectAbbr)) {
        throw new IngestAgainstDifferentProjectException(
            "Bag contains different project abbr than requested project. Bag value: " + bag.getBagData().getProject() + ". Requested project: " + projectAbbr + "."
        );
      }

      // 3. Pre-check DB conditions (Fast, Read-Only)
      // We check this NOW to avoid doing heavy File IO if the object already exists.
      String objectId = bag.getBagData().getId();
      if (digitalObjectRepository.existsById(objectId)) {
        throw new IngestObjectAlreadyExistsException(objectId);
      }

      // 4. Convert to Entities (CPU only, no DB)
      // We create the objects here to derive IDs for file storage
      DigitalObject digitalObject = conversionService.convert(bag.getBagData(), DigitalObject.class);
      if (digitalObject == null) {
        throw new IngestProcessingException(
            "Digital object domain object is unexpectedly null after conversion from bag data. Object id: " + objectId
        );
      }

      List<Datastream> datastreams = new ArrayList<>();

      for (var contentFile : bag.getBagData().getContentFiles()) {
        Datastream ds = conversionService.convert(contentFile, Datastream.class);
        if (ds == null) {
          throw new IngestProcessingException(
              "Datastream domain object is unexpectedly null after conversion from bag data . At Object: " + objectId + ". Datastream: " + contentFile.getDsid() + ". Bagpath: " + contentFile.getBagpath()
          );
        }
        ds.setDigitalObject(digitalObject); // Needed to derive ID
        datastreams.add(ds);
      }

      // --- PHASE 2: HEAVY IO (No DB Transaction) ---
      log.info("Starting file persistence for object {}", objectId);

      List<DublinCoreEntry> dublinCoreEntries = new ArrayList<>();

      for (Datastream ds : datastreams) {

        // Find the source file in the temp bag
        BagFile bagFile = bag.findContentFileByDsid(ds.getDsid());
        Path sourcePath = bagDirPath.resolve(bagFile.getBagpath());

        // SPECIAL HANDLING: DUBLIN CORE (Read to RAM once, use twice)
        if (ds.getDsid().equals(GAMSDsid.DC.getValue())) {
          try {
            // 1. Read bytes into memory (DC is small, so this is safe)
            byte[] dcBytes = Files.readAllBytes(sourcePath);

            // 2. Save to Repository using bytes (avoid re-reading from disk)
            // Note: Ensure your repository has a save(byte[], id) method, or wrap in ByteArrayInputStream
            datastreamContentRepository.save(new ByteArrayInputStream(dcBytes), ds.deriveDatastreamId());
            writtenFiles.add(ds.deriveDatastreamId());

            // 3. Parse XML from memory
            dublinCoreEntries.addAll(
                parseDublinCore(dcBytes, digitalObject, projectAbbr)
            );

          } catch (IOException e) {
            throw new IngestProcessingException("Failed to process DC file: " + sourcePath, e);
          }
        }
        // STANDARD HANDLING: ALL OTHER FILES (Stream to avoid OOM)
        else {
          try (InputStream in = Files.newInputStream(sourcePath)) {
            datastreamContentRepository.save(in, ds.deriveDatastreamId());
            writtenFiles.add(ds.deriveDatastreamId());
          } catch (IOException e) {
            throw new IngestProcessingException("Failed to stream file: " + sourcePath, e);
          }
        }

      }

      // --- PHASE 3: METADATA PERSISTENCE (Transactional) ---
      // Now the DB transaction will be very short (milliseconds)
      ingestPersistenceService.persistIngest(
          projectAbbr,
          digitalObject,
          datastreams,
          new ArrayList<>(dublinCoreEntries),
          bag
      );

    } catch (Exception e) {
      // --- COMPENSATION: MANUAL ROLLBACK ---
      log.error("Ingest failed. Rolling back {} written files.", writtenFiles.size());
      for (DatastreamId dsId : writtenFiles) {
        try {
          datastreamContentRepository.delete(dsId);
        } catch (Exception deleteEx) {
          log.error("Failed to cleanup orphaned file: {}", dsId, deleteEx);
        }
      }

      if (e instanceof IngestException) {
        // These are custom unchecked exceptions or already wrapped exceptions, re-throw directly
        throw (IngestException) e;
      }

      throw new IngestProcessingException("An unexpected error occurred during ingest: " + e.getMessage(), e);

    } finally {
      // Cleanup temp dir
      if (bagDirPath != null) ZipUtils.deleteDir(bagDirPath);
    }


  }

  /**
   * Export a digital object as a zipped BagIt package and write it to the provided OutputStream.
   *
   * @param objectId     the id of the digital object to export
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

    var submissionRecord = submissionRecordRepository.findById(objectId).orElseThrow(
        () -> {
          // TODO this is a server error - use different exception?
          return new DigitalObjectNotFoundException(
              "Cannot export digital object as bag: " + objectId
          );
        }
    );

    var datastreams = datastreamRepository.findAllByDigitalObject(digitalObject);

    if (datastreams.isEmpty()) {
      throw new ExportUnexpectedObjectStateException(
          "Cannot export digital object as bag - no datastreams found for object: " + objectId
      );
    }

    // 02. Map data to bag entities
    BagData bagData = BagData.from(digitalObject, datastreams, submissionRecord);
    BagMeta bagMeta = BagMeta.from(submissionRecord);
    BagInfo bagInfo = BagInfo.from(submissionRecord);

    // create bag from database entities
    Bag bag = new Bag(bagInfo, bagMeta, bagData);

    // sert bag data entry to indicate that the bag was created by the gams-api
    String createdBy = String.format("%s %s", buildProperties.getName(), buildProperties.getVersion());
    bag.getBagData().setCreatedBy(createdBy);

    // 03. write bag
    bag.writeAsZipToStream(outputStream, datastreamContentRepository);

  }

  /**
   * Helper to parse Dublin Core outside the main loop.
   */
  private List<DublinCoreEntry> parseDublinCore(byte[] xmlContent, DigitalObject digitalObject, String projectAbbr) {
    try {
      Document dublinCore = XMLUtils.parseXml(xmlContent);
      List<DublinCoreEntry> entries = new ArrayList<>();

      XMLUtils.extractDCElements(dublinCore).forEach(dcElement -> {
        entries.add(DublinCoreEntry.builder()
            .name(dcElement.getName())
            .value(dcElement.getValue())
            .language(dcElement.getLanguage())
            // We don't set .digitalObject(savedObject) here yet,
            // because the object isn't saved. We do that in Phase 3.
            .build());
      });
      return entries;
    } catch (Exception e) {
      throw new IngestProcessingException(
          "Failed to parse Dublin Core XML for project " + projectAbbr + ". Digital object: " + digitalObject.getId() + ". Error: " + e.getMessage(), e
      );
    }
  }

}
