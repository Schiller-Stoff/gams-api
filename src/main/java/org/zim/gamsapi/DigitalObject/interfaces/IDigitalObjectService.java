package org.zim.gamsapi.DigitalObject.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.Project.Project;

import java.util.List;

public interface IDigitalObjectService {

  DigitalObject save(DigitalObject digitalObject);

  List<DigitalObject> findAll();

  Page<DigitalObject> findAllByProjectAbbr(String projectAbbr, Pageable pageable);

  Page<DigitalObject> findAllByProjectAbbr(String projectAbbr, String containedInPid, Pageable pageable);

  List<DigitalObject> findAllByProjectAbbr(String projectAbbr);

  DigitalObject findById(String pid) throws DigitalObjectNotFoundException;

  void delete(DigitalObject digitalObject);

  void deleteAllForProject(Project project);

}
