package org.zim.gamsapi.DigitalObject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.DigitalObject.interfaces.IDigitalObjectService;
import org.zim.gamsapi.Project.Project;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DigitalObjectService implements IDigitalObjectService {

  private final IDigitalObjectRepository digitalObjectRepository;

  @Override
  @Transactional
  public DigitalObject save(DigitalObject digitalObject) {
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
    return digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbr(projectAbbr, pageable);
  }

  @Override
  public Page<DigitalObject> findAllByProjectAbbr(String projectAbbr, String containedInId, Pageable pageable) {
    return digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbrAndIdIsContainingIgnoreCase(projectAbbr, containedInId, pageable);
  }


  @Override
  public List<DigitalObject> findAllByProjectAbbr(String projectAbbr) {
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
    digitalObjectRepository.deleteAllByProject_ProjectAbbr(project.getProjectAbbr());
    log.info("Successfully deleted all digital objects for project {}", project);
  }


}
