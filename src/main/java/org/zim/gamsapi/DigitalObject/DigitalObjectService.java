package org.zim.gamsapi.DigitalObject;

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
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamMainResourceView;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamDetailsView;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntryCompactDTO;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntrySummaryView;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.zim.gamsapi.DigitalObject.dto.DigitalObjectCompactDTO;
import org.zim.gamsapi.DigitalObject.dto.DigitalObjectSearchResultDTO;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectConversionException;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectIdView;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.DigitalObject.interfaces.IDigitalObjectService;
import org.zim.gamsapi.Project.exceptions.ProjectNotFoundException;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.System.dto.PagedResponse;

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

  @Override
  @Transactional
  public DigitalObject save(DigitalObject digitalObject) {
    projectRepository.findById(digitalObject.getProject().getProjectAbbr()).orElseThrow(
            () -> {
              String msg = String.format("Aborting saving of digital object. Cannot find project %s for digital object %s",digitalObject.getProject().getProjectAbbr(), digitalObject );
              log.error(msg);
              return new ProjectNotFoundException(msg);
            }
    );

    DigitalObject savedObject = digitalObjectRepository.save(digitalObject);
    applicationEventPublisher.publishEvent(
        new DigitalObjectCreatedEvent(this, savedObject)
    );
    return savedObject;
  }

  @Override
  @Transactional
  public PagedResponse<DigitalObjectListItemView> findAllByProjectAbbr(String projectAbbr, String containedInId, Pageable pageable) {
    // TODO write unit + integration tests!
    projectRepository.findById(projectAbbr).orElseThrow(
            () -> {
              String msg = String.format("Aborting find all digital objects via project abbreviation. Cannot find project %s.",projectAbbr);
              log.error(msg);
              return new ProjectNotFoundException(msg);
            }
    );
    return PagedResponse.from(
        digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbrAndIdIsContainingIgnoreCase(projectAbbr, containedInId, pageable)
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
    DigitalObject foundObject =  digitalObjectRepository.findById(id).orElseThrow(() -> {
      String msg = String.format("Cannot find digital object via id: %s", id);
      log.info(msg);
      return new DigitalObjectNotFoundException(msg);
    });
    log.info("Found object in database {}", foundObject);
    return foundObject;
  }

  @Override
  @Transactional
  public void delete(DigitalObject digitalObject) {

    if(!digitalObjectRepository.existsById(digitalObject.getId())){
      String msg = String.format("Failed to delete digital object with id %s. It does not exist!", digitalObject.getId());
      log.error(msg);
      throw new DigitalObjectNotFoundException(msg);
    }

    Set<Datastream> datastreams = datastreamRepository.findAllByDigitalObject(digitalObject);
    datastreamRepository.deleteAllByDigitalObject(digitalObject);

    // TODO missing transaction exception to be thrown?
    datastreams.forEach(datastream -> {
      fileSystemRepository.delete(datastream.deriveDatastreamId());
    });

    dublinCoreEntryRepository.deleteAllByDigitalObject(digitalObject);

    digitalObjectRepository.delete(digitalObject);
    log.info("Successfully deleted digital object {}", digitalObject);
  }


    @Override
    @Transactional
    public PagedResponse<DigitalObjectListItemView> findAllByProjectAbbr(String projectAbbr, Optional<String> objectType, Pageable pageable) {
        projectRepository.findById(projectAbbr).orElseThrow(
                () -> {
                    String msg = String.format("Aborting find all digital objects via project abbreviation. Cannot find project %s.",projectAbbr);
                    log.error(msg);
                    return new ProjectNotFoundException(msg);
                }
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
    public PagedResponse<DigitalObjectListItemView> searchByDCFulltext(Set<String> projectAbbrs, Set<String> dcEntryNames, String fulltext, Pageable pageAble) {
      if(dcEntryNames.isEmpty()){
        String msg = String.format("No concrete dublin core elements specified - fulltext-searching over all dc fields. Trying to find digital objects by project abbreviations %s and fulltext %s", projectAbbrs, fulltext);
        log.trace(msg);
        return PagedResponse.from(
            dublinCoreEntryRepository.findDigitalObjectsByDCFulltext(projectAbbrs, fulltext, pageAble)
        );
      }
      String msg = String.format("Dublin core elements for fulltext-search specified - Trying to find digital objects by project abbreviation %s and dublin core entry names %s and fulltext %s", projectAbbrs, dcEntryNames, fulltext);
      log.trace(msg);
      return PagedResponse.from(
          dublinCoreEntryRepository.findDigitalObjectsByFulltextOnSpecificElements(projectAbbrs, dcEntryNames, fulltext, pageAble)
      );

    }

    @Override
    public DigitalObjectCompactDTO findDigitalObjectCompactDTOById(String digitalObjectId) {
      var foundObject = digitalObjectRepository.findDigitalObjectById(digitalObjectId).orElseThrow(
          () -> {
            String msg = String.format("Cannot find digital object via id: %s", digitalObjectId);
            log.info(msg);
            return new DigitalObjectNotFoundException(msg);
          });

      // converting details view to compactDTO
      DigitalObjectCompactDTO digitalObjectCompactDTO = conversionService.convert(foundObject,
          DigitalObjectCompactDTO.class);
      if (digitalObjectCompactDTO == null) {
        String msg = String.format(
            "Failed to convert DigitalObjectDetailsView to DigitalObjectCompactDTO. For object %s",
            digitalObjectId);
        log.error(msg);
        throw new DigitalObjectConversionException(msg);
      }

      // setting found datastreams
      var foundDatastreams = datastreamRepository.findAllByDigitalObjectId(digitalObjectId);
      digitalObjectCompactDTO.setDatastreams(
          foundDatastreams
              .stream()
              .map(
                  IDatastreamDetailsView::getDsid)
              .collect(Collectors.toList())
      );

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
        String msg = String.format("Failed to convert DigitalObject to DigitalObjectCompactDTO for object %s", digitalObject.getId());
        log.error(msg);
        throw new DigitalObjectConversionException(msg);
      }

      // Set Dublin Core entries
      Map<String, List<DublinCoreEntryCompactDTO>> dcMap = new HashMap<>();
      dublinCoreEntryRepository.findByDigitalObjectId(digitalObject.getId())
          .forEach(entry -> {
            DublinCoreEntryCompactDTO converted = conversionService.convert(entry, DublinCoreEntryCompactDTO.class);
            dcMap.computeIfAbsent(entry.getName(), k -> new ArrayList<>()).add(converted);
          });
      dto.setDublinCore(dcMap);

      dto.setMainResource(
          mainDatastreams.getOrDefault(digitalObject.getId(), null)
      );

      return dto;
    });




    return PagedResponse.from(mappedObjects);
  }

}
