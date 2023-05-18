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
    return digitalObjectRepository.findDigitalObjectsByProjectAbbr(projectAbbr, pageable);
  }

  @Override
  public Page<DigitalObject> findAllByProjectAbbr(String projectAbbr, String containedInPid, Pageable pageable) {
    return digitalObjectRepository.findDigitalObjectsByProjectAbbrAndPidIsContainingIgnoreCase(projectAbbr, containedInPid, pageable);
  }


  @Override
  public List<DigitalObject> findAllByProjectAbbr(String projectAbbr) {
    return digitalObjectRepository.findDigitalObjectsByProjectAbbr(projectAbbr);
  }

  @Override
  public DigitalObject findByPid(String pid) throws DigitalObjectNotFoundException {
    DigitalObject foundObject =  digitalObjectRepository.findById(pid).orElseThrow(() -> {
      String msg = String.format("Cannot find digital object via pid: %s", pid);
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
    digitalObjectRepository.deleteAllByProjectAbbr(project.getProjectAbbr());
    log.info("Successfully deleted all digital objects for project {}", project);
  }


}
