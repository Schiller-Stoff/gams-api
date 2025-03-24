package org.zim.gamsapi.DigitalObject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectChildSelfReferenceException;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectDetailsView;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectIdView;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.DigitalObject.interfaces.IDigitalObjectService;
import org.zim.gamsapi.Project.exceptions.ProjectNotFoundException;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class DigitalObjectService implements IDigitalObjectService {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;
  private final IProjectRepository projectRepository;
  private final IDatastreamContentRepository fileSystemRepository;
  private final IDublinCoreEntryRepository dublinCoreEntryRepository;

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

    return digitalObjectRepository.save(digitalObject);
  }

  @Override
  @Transactional
  public List<DigitalObject> findAll() {
    List<DigitalObject> digitalObjects = new ArrayList<>();
    digitalObjectRepository.findAll().forEach(digitalObjects::add);
    return digitalObjects;
  }

  @Override
  @Transactional
  public Page<DigitalObjectListItemView> findAllByProjectAbbr(String projectAbbr, Pageable pageable) {

    projectRepository.findById(projectAbbr).orElseThrow(
      () -> {
        String msg = String.format("Aborting find all digital objects via project abbreviation. Cannot find project %s.",projectAbbr);
        log.error(msg);
        return new ProjectNotFoundException(msg);
      }
    );

    return digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbr(projectAbbr, pageable);
  }

  @Override
  @Transactional
  public Page<DigitalObjectListItemView> findAllByProjectAbbr(String projectAbbr, String containedInId, Pageable pageable) {
    projectRepository.findById(projectAbbr).orElseThrow(
            () -> {
              String msg = String.format("Aborting find all digital objects via project abbreviation. Cannot find project %s.",projectAbbr);
              log.error(msg);
              return new ProjectNotFoundException(msg);
            }
    );
    return digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbrAndIdIsContainingIgnoreCase(projectAbbr, containedInId, pageable);
  }


  @Override
  @Transactional
  public List<DigitalObject> findAllByProjectAbbr(String projectAbbr) {
    projectRepository.findById(projectAbbr).orElseThrow(
            () -> {
              String msg = String.format("Aborting find all digital objects via project abbreviation. Cannot find project %s.",projectAbbr);
              log.error(msg);
              return new ProjectNotFoundException(msg);
            }
    );
    return digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbr(projectAbbr);
  }

  @Override
  public List<String> findAllIdsByProjectAbbr(String projectAbbr) {
    return digitalObjectRepository
        .findAllByProject_ProjectAbbr(projectAbbr)
        .stream()
        .map(DigitalObjectIdView::getId)
        .toList();
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
    public Page<DigitalObjectListItemView> findAllByProjectAbbr(String projectAbbr, Optional<String> objectType, Pageable pageable) {
        projectRepository.findById(projectAbbr).orElseThrow(
                () -> {
                    String msg = String.format("Aborting find all digital objects via project abbreviation. Cannot find project %s.",projectAbbr);
                    log.error(msg);
                    return new ProjectNotFoundException(msg);
                }
        );

        // search for all objects
        if(objectType.isEmpty()){
            return digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbr(projectAbbr, pageable);
        }

        // search for all objects with given object type and types
        return digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbrAndObjectType(projectAbbr, objectType.get(), pageable);

    }

    @Override
    @Transactional
    public DigitalObjectDetailsView findDigitalObjectDetailsViewById(String id) {
        return digitalObjectRepository.findDigitalObjectById(id).orElseThrow(
                () -> {
                    String msg = String.format("Cannot find digital object via id: %s", id);
                    log.info(msg);
                    return new DigitalObjectNotFoundException(msg);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DigitalObjectListItemView> searchObjectsByDublincCoreTags(List<String> projectAbbrs, String dcEntryName, List<String> dcEntryValue, Pageable pageAble){
      String msg = String.format("Trying to find digital objects by project abbreviation %s and dublin core entry name %s and values %s", projectAbbrs, dcEntryName, dcEntryValue);
      log.trace(msg);
      return dublinCoreEntryRepository.findDigitalObjectListItemViewsByProjectAbbrsAndDublinCoreElementFixedValues(projectAbbrs, dcEntryName, dcEntryValue, pageAble);
    }

    @Override
    public Page<DigitalObjectListItemView> searchByDCFulltext(Set<String> projectAbbrs, Set<String> dcEntryNames, String fulltext, Pageable pageAble) {
      if(dcEntryNames.isEmpty()){
        String msg = String.format("No concrete dublin core elements specified - fulltext-searching over all dc fields. Trying to find digital objects by project abbreviations %s and fulltext %s", projectAbbrs, fulltext);
        log.trace(msg);
        return dublinCoreEntryRepository.findDigitalObjectsByDCFulltext(projectAbbrs, fulltext, pageAble);
      }
      String msg = String.format("Dublin core elements for fulltext-search specified - Trying to find digital objects by project abbreviation %s and dublin core entry names %s and fulltext %s", projectAbbrs, dcEntryNames, fulltext);
      log.trace(msg);
      return dublinCoreEntryRepository.findDigitalObjectsByFulltextOnSpecificElements(projectAbbrs, dcEntryNames, fulltext, pageAble);

    }
}
