package org.zim.gamsapi.DigitalObject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface IDigitalObjectRepository extends CrudRepository<DigitalObject, String> {

  void deleteAllByProject_ProjectAbbr(String projectAbbr);

  List<DigitalObject> findDigitalObjectsByProject_ProjectAbbr(String projectAbbr);

  Page<DigitalObject> findDigitalObjectsByProject_ProjectAbbr(String projectAbbr, Pageable pageable);

  Page<DigitalObject> findDigitalObjectsByProject_ProjectAbbrAndIdIsContainingIgnoreCase(String projectAbbr, String id, Pageable pageable);

}
