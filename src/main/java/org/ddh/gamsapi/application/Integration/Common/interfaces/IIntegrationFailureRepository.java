package org.ddh.gamsapi.application.Integration.Common.interfaces;

import org.ddh.gamsapi.application.Integration.Common.IntegrationFailure;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface IIntegrationFailureRepository extends CrudRepository<IntegrationFailure, String>, JpaSpecificationExecutor<DigitalObject> {

  List<IntegrationFailure> findByStatusAndNextRetryAtBefore(String status, java.time.LocalDateTime time);

}
