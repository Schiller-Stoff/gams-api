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
import org.zim.gamsapi.DigitalObject.interfaces.IDigitalObjectService;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.exceptions.ProjectNotFoundException;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import java.util.ArrayList;
import java.util.List;
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

    // throw if child objects contain a self reference
    if(digitalObject.getChildObjects().contains(digitalObject)){
      String msg = String.format("Detected self reference in digital object's child objects. At digital object with pid: %s", digitalObject.getId());
      log.error(msg);
      throw new DigitalObjectChildSelfReferenceException(msg);
    }

    // referenced child object MUST EXIST
    digitalObject.getChildObjects().forEach(childObject -> {
      log.info("Checking child object: {} of parent: {}", childObject, digitalObject);
        if(!digitalObjectRepository.existsById(childObject.getId())){
          String msg = String.format("Cannot find contained child object %s in parent digital object %s", childObject.getId(), digitalObject.getId());
          log.error(msg);
          throw new DigitalObjectNotFoundException(msg);
        }
    });

    return digitalObjectRepository.save(digitalObject);
  }

  @Override
  public List<DigitalObject> findAll() {
    List<DigitalObject> digitalObjects = new ArrayList<>();
    digitalObjectRepository.findAll().forEach(digitalObjects::add);
    return digitalObjects;
  }

  @Override
  public Page<DigitalObject> findAllByProjectAbbr(String projectAbbr, Pageable pageable) {

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
  public Page<DigitalObject> findAllByProjectAbbr(String projectAbbr, String containedInId, Pageable pageable) {
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
  public DigitalObject assignChildObjects(DigitalObject parentObject, Set<DigitalObject> childObjects) {

   DigitalObject foundParentObject = digitalObjectRepository.findById(parentObject.getId()).orElseThrow(
        () -> {
          String msg = String.format("Aborting assign child objects. Cannot find parent object %s", parentObject);
          log.error(msg);
          return new DigitalObjectNotFoundException(msg);
        }
    );

   // assign child objects
   foundParentObject.setChildObjects(childObjects);
  // DON'T NEED / MUST NOT EXTRA SAVE BECAUSE ALREADY PERSISTED BY CONTEXT e.g. digitalObjectRepository.save(foundParentObject);
  // via findById() object is already managed by persistence context (if marked as @transactional)
  // https://www.baeldung.com/hibernate-entity-lifecycle#managed-entity

   log.info("Successfully assigned child objects {} to parent object {}", childObjects, foundParentObject);

   return foundParentObject;
  }

}
