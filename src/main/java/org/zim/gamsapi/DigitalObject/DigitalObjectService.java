package org.zim.gamsapi.DigitalObject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectChildSelfReferenceException;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectDetailsView;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.DigitalObject.interfaces.IDigitalObjectService;
import org.zim.gamsapi.Project.Project;
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

    if(digitalObject.getParent() != null){
      // throw if parent contains a self reference
      if(digitalObject.getParent().equals(digitalObject)){
        String msg = String.format("Detected self reference in digital object's parent object. At digital object with pid: %s", digitalObject.getId());
        log.error(msg);
        throw new DigitalObjectChildSelfReferenceException(msg);
      }

      // referenced parent object must exist
      if(!digitalObjectRepository.existsById(digitalObject.getParent().getId())){
        String msg = String.format("Cannot find contained parent object %s in digital object %s", digitalObject.getParent().getId(), digitalObject.getId());
        log.error(msg);
        throw new DigitalObjectNotFoundException(msg);
      }
    }


    return digitalObjectRepository.save(digitalObject);
  }

  @Override
  public List<DigitalObject> findAll() {
    List<DigitalObject> digitalObjects = new ArrayList<>();
    digitalObjectRepository.findAll().forEach(digitalObjects::add);
    return digitalObjects;
  }

  @Override
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
  public void delete(DigitalObject digitalObject) {
    digitalObjectRepository.delete(digitalObject);
  }

  @Override
  @Transactional
  public void deleteAllForProject(Project project) {
    // need to delete all the datastreams first --> otherwise constraint violation.
    // using custom performant query for large batch operations
    datastreamRepository.deleteAll(project.getProjectAbbr());
    digitalObjectRepository.deleteAll(project.getProjectAbbr());
    log.info("Successfully deleted all digital objects for project {}", project);
  }

  @Transactional
  @Override
  public DigitalObject assignParentObject(DigitalObject digitalObject, DigitalObject parent) {

   DigitalObject foundObject = digitalObjectRepository.findById(digitalObject.getId()).orElseThrow(
        () -> {
          String msg = String.format("Aborting assign parent object. Cannot find object %s", digitalObject);
          log.error(msg);
          return new DigitalObjectNotFoundException(msg);
        }
    );

   // assign child objects
    foundObject.setParent(parent);
  // DON'T NEED / MUST NOT EXTRA SAVE BECAUSE ALREADY PERSISTED BY CONTEXT e.g. digitalObjectRepository.save(foundParentObject);
  // via findById() object is already managed by persistence context (if marked as @transactional)
  // https://www.baeldung.com/hibernate-entity-lifecycle#managed-entity

   log.info("Successfully assigned parent object {} to object {}", parent, foundObject);

   return foundObject;
  }


    @Override
    public Page<DigitalObjectListItemView> findAllByProjectAbbr(String projectAbbr, Optional<String> objectType, Optional<Set<String>> types, Pageable pageable) {
        projectRepository.findById(projectAbbr).orElseThrow(
                () -> {
                    String msg = String.format("Aborting find all digital objects via project abbreviation. Cannot find project %s.",projectAbbr);
                    log.error(msg);
                    return new ProjectNotFoundException(msg);
                }
        );

        // search for all objects
        if(objectType.isEmpty() && types.isEmpty()){
            return digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbr(projectAbbr, pageable);
        }

        // search for all objects with given object type
        if(objectType.isPresent() && types.isEmpty()){
            return digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbrAndObjectType(projectAbbr, objectType.get(), pageable);
        }

        // search for all objects with given types
        if(objectType.isEmpty() && types.isPresent()){
            return digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbrAndTypesIn(projectAbbr, types.get(), pageable);
        }

        // search for all objects with given object type and types
        return digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbrAndObjectTypeAndTypesIn(projectAbbr, objectType.get(), types.get(), pageable);

    }

    @Override
    public DigitalObjectDetailsView findDigitalObjectDetailsViewById(String id) {
        return digitalObjectRepository.findDigitalObjectById(id).orElseThrow(
                () -> {
                    String msg = String.format("Cannot find digital object via id: %s", id);
                    log.info(msg);
                    return new DigitalObjectNotFoundException(msg);
                });
    }
}
