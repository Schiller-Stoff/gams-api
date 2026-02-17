package org.ddh.gamsapi.domain.DigitalObject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.Datastream.Datastream;
import org.ddh.gamsapi.domain.Datastream.DatastreamBuilder;
import org.ddh.gamsapi.domain.Datastream.utils.GAMSDsid;
import org.ddh.gamsapi.domain.Datastream.utils.dto.DatastreamMainResourceDto;
import org.ddh.gamsapi.domain.Datastream.utils.exceptions.DatastreamCannotWriteFileException;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamMainResourceView;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord.IArchivalRecordRepository;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.DublinCoreEntry;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.DublinCoreEntryCompactDTO;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.DublinCoreEntrySummaryView;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.ddh.gamsapi.domain.DigitalObject.SubmissionRecord.ISubmissionRecordRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.dto.DigitalObjectCompactDTO;
import org.ddh.gamsapi.domain.DigitalObject.utils.dto.DigitalObjectCreateDto;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectAlreadyExistsException;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectConversionException;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.DigitalObjectIdView;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.DigitalObjectListItemView;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectService;
import org.ddh.gamsapi.domain.MetadataBaseEntityBuilder;
import org.ddh.gamsapi.domain.Project.Project;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectNotFoundException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.ddh.gamsapi.infrastructure.System.dto.PagedResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class DigitalObjectService implements IDigitalObjectService {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;
  private final IProjectRepository projectRepository;
  private final IDatastreamContentRepository fileSystemRepository;
  private final IDublinCoreEntryRepository dublinCoreEntryRepository;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final ConversionService conversionService;
  private final ISubmissionRecordRepository submissionRecordRepository;
  private final IArchivalRecordRepository archivalRecordRepository;
  private final IDatastreamContentRepository datastreamContentRepository;

  @Override
  @Transactional
  public DigitalObject save(DigitalObject digitalObject) {
    var foundProject = projectRepository.findById(digitalObject.getProject().getProjectAbbr()).orElseThrow(
            () -> new ProjectNotFoundException(
                "Aborting saving of digital object. Cannot find project "
                    + digitalObject.getProject().getProjectAbbr()
                    + " for digital object "
                    + digitalObject
            )
    );

    DigitalObject savedObject = digitalObjectRepository.save(digitalObject);
    foundProject.setContentLastModified(new Date());
    applicationEventPublisher.publishEvent(
        new DigitalObjectCreatedEvent(this, savedObject)
    );
    return savedObject;
  }

  @Override
  @Transactional
  public PagedResponse<DigitalObjectListItemView> findAllByProjectAbbr(String projectAbbr, String idSearchTerm, Pageable pageable) {
    if(!projectRepository.existsById(projectAbbr)){
      throw new ProjectNotFoundException(
          "Aborting find all digital objects via project abbreviation. Cannot find project "
              + projectAbbr
              + "."
      );
    }

    // Normalize search term
    String normalized = idSearchTerm.trim();

    // Strategy 2: Prefix search (fast, uses index)
    return PagedResponse.from(
        digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbrAndIdStartingWith(
            projectAbbr,
            normalized,
            pageable
        )
    );
  }

  @Override
  public PagedResponse<String> findAllIdsByProjectAbbr(String projectAbbr, Pageable pageable) {

    var mappedIdsPaginated = digitalObjectRepository
        .findAllByProject_ProjectAbbr(projectAbbr, pageable)
        .map(DigitalObjectIdView::getId);

    return PagedResponse.from(mappedIdsPaginated);
  }

  @Override
  @Transactional
  public DigitalObject findById(String id) throws DigitalObjectNotFoundException {
    DigitalObject foundObject =  digitalObjectRepository.findById(id).orElseThrow(() -> new DigitalObjectNotFoundException(
        "Cannot find digital object via id: " + id
    ));
    log.info("Found object {}", foundObject);
    return foundObject;
  }

  @Override
  @Transactional
  public void delete(DigitalObject digitalObject) {

    var foundProject = projectRepository.findById(digitalObject.getProject().getProjectAbbr()).orElseThrow(
        () -> new ProjectNotFoundException(
            "Cannot delete digital object "
                + digitalObject
                + ". Project "
                + digitalObject.getProject().getProjectAbbr()
                + " does not exist!"
        )
    );

    if(!digitalObjectRepository.existsById(digitalObject.getId())){
      throw new DigitalObjectNotFoundException(
          "Failed to delete digital object with id "
              + digitalObject.getId()
              + ". It does not exist!"
      );
    }

    submissionRecordRepository.deleteById(digitalObject.getId());

    archivalRecordRepository.deleteAllByDigitalObjectId(digitalObject.getId());

    Set<Datastream> datastreams = datastreamRepository.findAllByDigitalObject(digitalObject);
    datastreamRepository.deleteAllByDigitalObject(digitalObject);

    // TODO missing transaction exception to be thrown?
    // TODO needs refactoring using the failed delete event
    datastreams.forEach(datastream -> {
      fileSystemRepository.delete(datastream.deriveDatastreamId());
    });

    dublinCoreEntryRepository.deleteAllByDigitalObject(digitalObject);

    digitalObjectRepository.delete(digitalObject);

    foundProject.setContentLastModified(new Date());

    log.info("Successfully deleted digital object {}", digitalObject.getId());
  }


    @Override
    @Transactional
    public PagedResponse<DigitalObjectListItemView> findAllByProjectAbbr(String projectAbbr, Optional<String> objectType, Pageable pageable) {
        projectRepository.findById(projectAbbr).orElseThrow(
                () -> new ProjectNotFoundException(
                    "Aborting find all digital objects via project abbreviation. Cannot find project "
                        + projectAbbr
                        + "."
                )
        );

        // search for all objects
        if(objectType.isEmpty()){
          return PagedResponse.from(
              digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbr(projectAbbr, pageable)
          );
        }

        // search for all objects with given object type and types
        return PagedResponse.from(
            digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbrAndObjectType(projectAbbr, objectType.get(), pageable)
        );

    }

    @Override
    public DigitalObjectCompactDTO findDigitalObjectCompactDTOById(String digitalObjectId) {
      var foundObject = digitalObjectRepository.findDigitalObjectById(digitalObjectId).orElseThrow(
          () -> new DigitalObjectNotFoundException(
              "Cannot find digital object via id: " + digitalObjectId
          ));

      // converting details view to compactDTO
      DigitalObjectCompactDTO digitalObjectCompactDTO = conversionService.convert(foundObject,
          DigitalObjectCompactDTO.class);
      if (digitalObjectCompactDTO == null) {
        throw new DigitalObjectConversionException(
            "Failed to convert DigitalObjectDetailsView to DigitalObjectCompactDTO. For object "
                + digitalObjectId
        );
      }

      // setting main resource if it exists
      var mainDatastreams = datastreamRepository.findMainDatastreamsByDigitalObjectIds(
          Set.of(digitalObjectId)
      );

      if(mainDatastreams.size() > 1){
        String msg = String.format("Found more than one main datastream for digital object %s. This is not expected!", digitalObjectId);
        log.warn(msg);
      }

      if (!mainDatastreams.isEmpty()) {
        IDatastreamMainResourceView mainDatastream = mainDatastreams.get(0);
        digitalObjectCompactDTO.setMainResource(
            conversionService.convert(mainDatastream, DatastreamMainResourceDto.class)
        );
      }

      // setting found dublin core entries
      var foundDublinCoreEntries = dublinCoreEntryRepository.findByDigitalObjectId(digitalObjectId);
      // convert to map with name as key
      Map<String, List<DublinCoreEntryCompactDTO>> dcMap = new HashMap<>();
      for (DublinCoreEntrySummaryView entry : foundDublinCoreEntries) {
        DublinCoreEntryCompactDTO converted = conversionService.convert(entry, DublinCoreEntryCompactDTO.class);
        dcMap.computeIfAbsent(entry.getName(), k -> new ArrayList<>()).add(converted);
      }

      digitalObjectCompactDTO.setDublinCore(dcMap);
      return digitalObjectCompactDTO;
    }

  public PagedResponse<DigitalObjectListItemView> findAllByProjectAndTags(
      String projectAbbr,
      Set<String> tags,
      Pageable pageable
  ) {

    if (!projectRepository.existsById(projectAbbr)) {
      throw new ProjectNotFoundException(
          "Cannot find digital objects. Project does not exist: " + projectAbbr
      );
    }

    if (tags == null || tags.isEmpty()) {
      // No tags = return all objects in project
      return PagedResponse.from(
          digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbr(projectAbbr, pageable)
      );
    }

    // AND-based tag filtering
    Page<DigitalObjectListItemView> result = digitalObjectRepository
        .findByProject_ProjectAbbrAndTagsIn(projectAbbr, tags, tags.size(), pageable);

    return PagedResponse.from(result);
  }


  @Override
  @Transactional(readOnly = true)
  public Set<String> findDistinctTagsByProject(String projectAbbr) {
    if (!projectRepository.existsById(projectAbbr)) {
      throw new ProjectNotFoundException(
          "Cannot find tags. Project does not exist: " + projectAbbr
      );
    }

    return digitalObjectRepository.findDistinctTagsByProjectAbbr(projectAbbr);
  }


  /**
   * TODO test is missing
   * @param projectAbbr project abbreviation
   * @param dto command object to create a digital object
   * @return saved digital object
   */
  @Transactional
  public DigitalObject create(String projectAbbr, DigitalObjectCreateDto dto) {

    Project project = projectRepository.findById(projectAbbr)
        .orElseThrow(() -> new ProjectNotFoundException(
            "Cannot find project " + projectAbbr));

    String objectId = projectAbbr + "." + dto.getIdSuffix();

    if (digitalObjectRepository.existsById(objectId)) {
      throw new DigitalObjectAlreadyExistsException(
          "Digital object " + objectId + " already exists");
    }

    // 1. Create digital object
    DigitalObject digitalObject = DigitalObjectBuilder.builder()
        .id(objectId)
        .project(project)
        .publisher(dto.getPublisher())
        .funder(dto.getFunder())
        .objectType(dto.getObjectType())
        .baseMetadata(new MetadataBaseEntityBuilder()
            .title(dto.getTitle())
            .creator(dto.getCreator())
            .rights(dto.getRights())
            .description(dto.getDescription())
            .build())
        .build();

    DigitalObject savedObject = digitalObjectRepository.save(digitalObject);

    // 2. Generate and persist Dublin Core entries
    List<DublinCoreEntry> dcEntries = buildMinimalDublinCoreEntries(savedObject, dto);
    dublinCoreEntryRepository.saveAll(dcEntries);

    // 3. Generate DC.xml datastream content and persist
    byte[] dcXmlBytes = generateDublinCoreXml(dto);

    final String DC_DSID = GAMSDsid.DC.getValue();

    // create as datastream here?
    Datastream dcDatastream = DatastreamBuilder.builder()
        .digitalObject(savedObject)
        .dsid(DC_DSID)
        .baseMetadata(
            new MetadataBaseEntityBuilder()
                .title(savedObject.getBaseMetadata().getTitle())
                .creator(dto.getCreator())
                .rights(savedObject.getBaseMetadata().getRights())
                .description(savedObject.getBaseMetadata().getDescription())
                .build()
        )
        .lang(Set.of("en"))
        .bagPath(DC_DSID)
        .mimeType(MediaType.APPLICATION_XML_VALUE)
        .size((long) dcXmlBytes.length)
        .build();

    // datastream content repository
    try(
        InputStream inputStream = new ByteArrayInputStream(dcXmlBytes)
        ){
        var result = datastreamContentRepository.saveWithChecksums(inputStream, dcDatastream.deriveDatastreamId());
        dcDatastream.setMd5Checksum(result.md5Checksum());
        dcDatastream.setSha512Checksum(result.sha512Checksum());
        datastreamRepository.save(dcDatastream);

    } catch (IOException e){
      throw new DatastreamCannotWriteFileException(
          "Cannot create digital object with id " + digitalObject.getId() + " because datastream " + DC_DSID + " cannot be saved to disk. Cause: " + e.getMessage(),
          e);
    }

    // 4. Update project timestamp
    project.setContentLastModified(new Date());

    applicationEventPublisher.publishEvent(
        new DigitalObjectCreatedEvent(this, savedObject));

    return savedObject;
  }

  private List<DublinCoreEntry> buildMinimalDublinCoreEntries(
      DigitalObject savedObject, DigitalObjectCreateDto dto) {
    List<DublinCoreEntry> entries = new ArrayList<>();

    entries.add(DublinCoreEntry.builder()
        .digitalObject(savedObject).name("title").value(dto.getTitle()).build());
    entries.add(DublinCoreEntry.builder()
        .digitalObject(savedObject).name("creator").value(dto.getCreator()).build());
    entries.add(DublinCoreEntry.builder()
        .digitalObject(savedObject).name("rights").value(dto.getRights()).build());
    entries.add(DublinCoreEntry.builder()
        .digitalObject(savedObject).name("publisher").value(dto.getPublisher()).build());

    if (dto.getDescription() != null && !dto.getDescription().isEmpty()) {
      entries.add(DublinCoreEntry.builder()
          .digitalObject(savedObject).name("description").value(dto.getDescription()).build());
    }

    return entries;
  }

  private byte[] generateDublinCoreXml(DigitalObjectCreateDto dto) {
    // Generate minimal valid DC XML
    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xml.append("<dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n");
    xml.append("  <dc:title>").append(escapeXml(dto.getTitle())).append("</dc:title>\n");
    xml.append("  <dc:creator>").append(escapeXml(dto.getCreator())).append("</dc:creator>\n");
    xml.append("  <dc:rights>").append(escapeXml(dto.getRights())).append("</dc:rights>\n");
    xml.append("  <dc:publisher>").append(escapeXml(dto.getPublisher())).append("</dc:publisher>\n");
    if (dto.getDescription() != null && !dto.getDescription().isEmpty()) {
      xml.append("  <dc:description>").append(escapeXml(dto.getDescription())).append("</dc:description>\n");
    }
    xml.append("</dc:dc>");
    return xml.toString().getBytes(StandardCharsets.UTF_8);
  }

  private String escapeXml(String input) {
    if (input == null) return "";
    return input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }



}
