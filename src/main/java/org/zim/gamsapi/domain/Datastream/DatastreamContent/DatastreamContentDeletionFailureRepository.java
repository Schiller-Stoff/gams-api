package org.zim.gamsapi.domain.Datastream.DatastreamContent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface DatastreamContentDeletionFailureRepository extends JpaRepository<DatastreamContentDeletionFailure, Long> {

}
