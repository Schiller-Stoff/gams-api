package org.zim.gamsapi.SubInfoPack;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.exceptions.ProjectNotFoundException;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.SubInfoPack.exceptions.SubInfoPackProcessingException;
import org.zim.gamsapi.SubInfoPack.interfaces.ISubInfoPackService;
import org.zim.gamsapi.SubInfoPack.utils.MimeTypeDetector;
import org.zim.gamsapi.SubInfoPack.utils.XMLUtils;
import org.zim.gamsapi.SubInfoPack.utils.ZipUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
    switch (subInfoPack.getIngestProfile()) {
      case "simple" -> ingestSimple(subInfoPack);
      case "basic" -> ingestBasic(subInfoPack);
      default -> {
        String msg = String.format("There was no valid ingest type defined inside the submission information package %s", subInfoPack);
        log.error(msg);
        throw new SubInfoPackProcessingException(msg);
      }
    }
  }

  /**
   * Handles the simple type ingest operation.
   * @param subInfoPack submission information package to be processed.
   */
  private void ingestSimple(SubInfoPack subInfoPack){

    projectRepository.findById(subInfoPack.getProjectAbbr()).orElseThrow(
            () -> {
              String msg = String.format("Aborting ingest. Cannot find project %s", subInfoPack.getProjectAbbr());
              log.error(msg);
              return new ProjectNotFoundException(msg);
            }
    );

    AtomicReference<DigitalObject> digitalObject =  new AtomicReference<>();
    AtomicReference<Boolean> containsSourceXml = new AtomicReference<>(false);
    AtomicReference<Boolean> containsMetadataXml = new AtomicReference<>(false);

    // Construct id from the folder-name and the current project
    ZipUtils.walkZippedDir(subInfoPack.getZippedFolder(), (zipEntry, byteArrayOutputStream) -> {

      log.trace("Walking through zip entry during simple ingest: {}", zipEntry);

      // find root folder and save object and then skip further processing
      boolean isRootFolder = zipEntry.isDirectory() && (StringUtils.countOccurrencesOf(zipEntry.getName(), "/") == 1);
      if(isRootFolder){
        String folderName = zipEntry.getName().replace("/", "");
        //String id = String.format("o:%s.%s", subInfoPack.getProjectAbbr(), folderName);

        // no o:derla.sty1 --> just sty1
        digitalObject.set(DigitalObject
                .builder()
                .id(folderName)
                .project(Project.builder().projectAbbr(subInfoPack.getProjectAbbr()).build())
                .build());

        // if the object already exists --> delete (cascades to all datastreams)
        digitalObjectRepository.findById(folderName).ifPresent(foundObject -> {
          digitalObjectRepository.delete(foundObject);
          log.info("Found already existing digital object to ingest {}. Successfully deleted found digital object. Proceeding with ingest for SIP {}", foundObject, subInfoPack);
        });

        DigitalObject savedObject = digitalObjectRepository.save(digitalObject.get());
        log.info("Successfully saved digital object {} during ingest for SIP {}", savedObject, subInfoPack);
        return;
      }

      // skipping directories
      if(zipEntry.isDirectory())return;

      // skip nested files
      if(StringUtils.countOccurrencesOf(zipEntry.getName(), "/") > 1){
        log.warn("Skipping SIP file: {} Encountered more than 1 '/' inside filepath. Nested folders are not allowed. ", zipEntry.getName());
        return;
      }

      final String SPLIT_DELIMITER = "/";
      String[] split = StringUtils.split(zipEntry.getName(),SPLIT_DELIMITER);
      // skip nested file (another check)
      if(split == null){
        log.warn("Something went wrong at splitting the string {}. The split array is null. Used split delimiter: {}", zipEntry.getName(), SPLIT_DELIMITER);
        return;
      }
      // again skip nested files
      if(split.length > 2){
        log.debug("Ignoring file {} because nested folder structures inside SIP folder are not supported", zipEntry.getName());
        return;
      }

      //
      // Start processing of datastreams
      //

      String fileName = split[1];

      // process contained metadata.xml for digital objects
      if(fileName.equalsIgnoreCase("metadata.xml")){
        digitalObject.get().setBaseMetadata(
                extractMetaData(byteArrayOutputStream.toByteArray())
        );
        log.info("Successfully applied detected metadata.xml inside SIP {} for the object {}", subInfoPack, fileName);
        containsMetadataXml.set(true);
        return;
      }

      // check existence of source.xml
      if(fileName.equalsIgnoreCase("source.xml")){
        containsSourceXml.set(true);
        log.trace("Found source.xml inside given SIP {}", subInfoPack);
      }


      // process datastreams from here
      String dsid = fileName.replace(".", "_");
      String mimetype = MimeTypeDetector.forceDetect(
              new ByteArrayInputStream(byteArrayOutputStream.toByteArray()),fileName
      );

      Datastream datastreamToSave = Datastream.builder()
              .dsid(dsid)
              .data(byteArrayOutputStream.toByteArray())
              .mimeType(mimetype)
              .fileName(fileName)
              .digitalObject(digitalObject.get())
              .build();

      Datastream savedDatastream = datastreamRepository.save(datastreamToSave);
      // add datastream references to constructed digital object
      List<Datastream> datastreamList = digitalObject.get().getDatastreams();
      datastreamList.add(savedDatastream);
      log.info("Successfully saved datastream {} for SIP {}", savedDatastream, subInfoPack);
    });

    // validation after loop is completed

    if(!containsSourceXml.get()){
      String msg = String.format("Sent SIP does not contain the required source.xml - denying ingest. For SIP %s", subInfoPack);
      log.error(msg);
      throw new SubInfoPackProcessingException(msg);
    }

    if(!containsMetadataXml.get()){
      String msg = String.format("Sent SIP does not contain the required metadata.xml - denying ingest. For SIP %s", subInfoPack);
      log.error(msg);
      throw new SubInfoPackProcessingException(msg);
    }

    // again save for additional content being created (like metadata.xml)
    digitalObjectRepository.save(digitalObject.get());
    log.info("Successfully finished ingest simple operation for SIP: {}. Created digital object: {}", subInfoPack, digitalObject.get());
  }


  private void ingestBasic(SubInfoPack subInfoPack){
    throw new SubInfoPackProcessingException("The basic ingest is currently not supported / implemented");
    /*Document dublinCore;
    try {
      dublinCore = extractDCXml(subInfoPack.getZippedFolder());
    } catch (IOException e){
      String msg = String.format("Aborting ingest with cause: %s", e);
      log.error(msg);
      throw new SubInfoPackProcessingException(msg);
    }*/




    // TODO 1. save digital object via id from metadata.xml?


    // TODO 2 save datastreams


    // TODO finish implementation

  }




  /**
   * Extracts metadata.xml of given zipped SIP (=Submission information package)
   * @return parsed metadata.xml from zipped SIP (=Submission information package)
   * @throws IOException no dc.xml inside
   */
  private Document extractDCXml(byte[] sourceZip) throws IOException {
    AtomicReference<Document> sourceXml = new AtomicReference<>();
    ZipUtils.walkZippedDir(sourceZip, (zipEntry, outputStream) -> {
      log.debug("**Got individual ingest zip entry inside zip file: {}", zipEntry.getName());
      if(zipEntry.getName().contains("dc.xml")){
        sourceXml.set(XMLUtils.parseXml(outputStream.toByteArray()));
      }
    });

    // check if extraction of source xml actually worked!
    if(sourceXml.get() == null){
      String msg = "Failed to extract dc.xml from given ingest files (as zip). Aborting ingest for current object";
      log.error(msg);
      throw new IOException(msg);
    }

    return sourceXml.get();
  }


  /**
   * Extracts metadata from given dublin core xml and stores data inside dublin core wrapper class.
   * @param metadataXml metadata xml as byte array
   * @return built dublin core wrapper class.
   */
  private MetadataBaseEntity extractMetaData(byte[] metadataXml){

    Document parsedMetadataXml = XMLUtils.parseXml(metadataXml);
    // xpath returns all children of the root element
    NodeList children = XMLUtils.getAllXpath("//Description/*", parsedMetadataXml);
    MetadataBaseEntity metadataBaseEntity = MetadataBaseEntity.builder().build();

    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      String nodeValue = child.getTextContent();

      log.trace("Looping through node: {}", child.getNodeName());

      // TODO mapping could be done via jackson!!!!
      switch (child.getNodeName()){
        case "dh:title" -> metadataBaseEntity.setTitle(nodeValue);
        case "dh:creator" -> metadataBaseEntity.setCreator(nodeValue);
        case "dh:description" -> metadataBaseEntity.setDescription(nodeValue);
        //case "dh:subject" -> metadataBaseEntity.setSubject(addToNullableList(metadataBaseEntity.getSubject(), nodeValue));
        case "dh:publisher" -> metadataBaseEntity.setPublisher(nodeValue);
        //case "dh:contributor" -> metadataBaseEntity.setContributor(addToNullableList(metadataBaseEntity.getContributor(), nodeValue));
        //case "dh:date" -> metadataBaseEntity.setDate(addToNullableList(metadataBaseEntity.getDate(), nodeValue));
        //case "dh:type" -> metadataBaseEntity.setType(addToNullableList(metadataBaseEntity.getType(), nodeValue));
        //case "dh:format" -> metadataBaseEntity.setFormat(addToNullableList(metadataBaseEntity.getFormat(), nodeValue));
        //case "dh:source" -> metadataBaseEntity.setSource(addToNullableList(metadataBaseEntity.getSource(), nodeValue));
        //case "dh:language" -> metadataBaseEntity.setLanguage(addToNullableList(metadataBaseEntity.getLanguage(), nodeValue));
        //case "dh:relation" -> metadataBaseEntity.setRelation(addToNullableList(metadataBaseEntity.getRelation(), nodeValue));
        //case "dh:coverage" -> metadataBaseEntity.setCoverage(addToNullableList(metadataBaseEntity.getCoverage(), nodeValue));
        case "dh:rights" -> metadataBaseEntity.setRights(nodeValue);
        default -> log.warn("Metadata.xml ingest processing: Skipping unrecognized element {} with value {}", child.getNodeName(), nodeValue);
      }
    }

    log.info("Built: {}", metadataBaseEntity);
    return metadataBaseEntity;

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

}
