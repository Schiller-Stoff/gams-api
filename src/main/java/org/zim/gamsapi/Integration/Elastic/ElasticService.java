package org.zim.gamsapi.Integration.Elastic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.Integration.Common.exceptions.IntegrationServiceException;
import org.zim.gamsapi.Integration.Common.interfaces.IIntegrationService;

@Service
@Slf4j
@RequiredArgsConstructor
public class ElasticService implements IIntegrationService {

  private final ElasticDigitalObjectRepository digitalObjectElasticRepository;

  private final IDigitalObjectRepository digitalObjectRepository;

  public void indexObject(String projectId, String id) {
    log.info("Adding digital object to elasticsearch with ID: {}", id);

    // Fetch the digital object from the repository
    var digitalObject = digitalObjectRepository.findById(id)
        // TODO rethink logging
        .orElseThrow(() -> new IntegrationServiceException("Digital object not found with ID: " + id));

    ElasticDigitalObject elasticDigitalObject = new ElasticDigitalObject();
    elasticDigitalObject.setId(digitalObject.getId());
    digitalObjectElasticRepository.save(elasticDigitalObject);
  }

  @Override
  public void indexObjects(String projectAbbr) {
    // TODO implement indexing of all digital objects for a project
  }

  @Override
  public void deleteIndexedObjects(String projectAbbr) {
    // TODO implement deletion of all indexed digital objects for a project
  }

  @Override
  public void deleteIndexedObject(String projectAbbr, String id) {
    // TODO implement
  }
}
