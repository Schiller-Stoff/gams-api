package org.ddh.gamsapi.application.WebDeployment;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebDeploymentRepository extends CrudRepository<WebDeployment, String> {
}