package org.ddh.gamsapi.domain.DigitalObject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.ddh.gamsapi.domain.Datastream.Datastream;
import org.ddh.gamsapi.domain.Datastream.utils.dto.DatastreamMainResourceDto;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamMainResourceView;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.DublinCoreEntryCompactDTO;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.DublinCoreEntrySummaryView;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.dto.DigitalObjectCompactDTO;
import org.ddh.gamsapi.domain.DigitalObject.utils.dto.DigitalObjectSearchResultDTO;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectConversionException;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.DigitalObjectIdView;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.DigitalObjectListItemView;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectService;
import org.ddh.gamsapi.domain.DigitalObject.SubmissionRecord.ISubmissionRecordRepository;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectNotFoundException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.ddh.gamsapi.infrastructure.System.dto.PagedResponse;

import java.util.*;
import java.util.stream.Collectors;

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
  private final ISubmissionRecordRepository bagEntityRepository;
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

    bagEntityRepository.deleteById(digitalObject.getId());

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


  /**
   * Advanced Dublin Core search using Criteria API for complex multi-field queries.
   * This method supports:
   * - Multiple Dublin Core fields with multiple values each
   * - Different search modes (exact, contains, fulltext)
   * - Project filtering
   * - Type-safe query building
   * Use this for complex search scenarios with multiple criteria.
   *
   * @param dublinCoreFilters MultiValueMap of DC field names to search values
   * @param projectAbbrs Set of project abbreviations to filter by
   * @param searchMode Search mode (EXACT_MATCH, CONTAINS, FULLTEXT)
   * @param pageable Pagination information
   * @return Page of digital objects matching the criteria
   * TODO remove outdated method?
   */
  public PagedResponse<DigitalObjectSearchResultDTO> searchDigitalObjectsByDublinCoreCriteria(
      MultiValueMap<String, String> dublinCoreFilters,
      Set<String> projectAbbrs,
      DigitalObjectDublinCoreSpecification.SearchMode searchMode,
      Pageable pageable) {

    // TODO TESTS!

    log.debug("Searching digital objects with DC criteria: {}, projects: {}, mode: {}",
        dublinCoreFilters, projectAbbrs, searchMode);

    Specification<DigitalObject> spec = new DigitalObjectDublinCoreSpecification(
        dublinCoreFilters, projectAbbrs, searchMode);
    Page<DigitalObject> digitalObjects = digitalObjectRepository.findAll(spec, pageable);

    // Additionally fetch dublin core entries and the main datastreams

    // Extract IDs for batch fetching
    Set<String> digitalObjectIds = digitalObjects.getContent()
        .stream()
        .map(DigitalObject::getId)
        .collect(Collectors.toSet());

    Map<String, IDatastreamMainResourceView> mainDatastreams = datastreamRepository
        .findMainDatastreamsByDigitalObjectIds(digitalObjectIds)
        .stream()
        .collect(Collectors.toMap(
            ds -> ds.getDigitalObject().getId(),
            ds -> ds
        ));


    var mappedObjects = digitalObjects.map(digitalObject -> {
      // Convert to DTO
      var dto = conversionService.convert(digitalObject, DigitalObjectSearchResultDTO.class);
      if (dto == null) {
        throw new DigitalObjectConversionException(
            "Failed to convert DigitalObject to DigitalObjectCompactDTO for object " + digitalObject.getId()
        );
      }

      // Set Dublin Core entries
      Map<String, List<DublinCoreEntryCompactDTO>> dcMap = new HashMap<>();
      dublinCoreEntryRepository.findByDigitalObjectId(digitalObject.getId())
          .forEach(entry -> {
            DublinCoreEntryCompactDTO converted = conversionService.convert(entry, DublinCoreEntryCompactDTO.class);
            dcMap.computeIfAbsent(entry.getName(), k -> new ArrayList<>()).add(converted);
          });
      dto.setDublinCore(dcMap);

      var foundMainDatastream = mainDatastreams
          .getOrDefault(digitalObject.getId(), null);
      if (foundMainDatastream != null) {
        // Set main resource if available
        dto.setMainResource(
            conversionService.convert(foundMainDatastream, DatastreamMainResourceDto.class)
        );
      }


      return dto;
    });




    return PagedResponse.from(mappedObjects);
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



}
