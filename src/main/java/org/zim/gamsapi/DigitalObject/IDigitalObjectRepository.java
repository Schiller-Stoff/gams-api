package org.zim.gamsapi.DigitalObject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface IDigitalObjectRepository extends CrudRepository<DigitalObject, String> {

  void deleteAllByProjectAbbr(String projectAbbr);

  List<DigitalObject> findDigitalObjectsByProjectAbbr(String projectAbbr);

  Page<DigitalObject> findDigitalObjectsByProjectAbbr(String projectAbbr, Pageable pageable);

  Page<DigitalObject> findDigitalObjectsByProjectAbbrAndIdIsContainingIgnoreCase(String projectAbbr, String id, Pageable pageable);

}
