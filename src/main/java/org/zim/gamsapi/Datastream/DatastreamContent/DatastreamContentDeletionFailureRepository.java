package org.zim.gamsapi.Datastream.DatastreamContent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface DatastreamContentDeletionFailureRepository extends JpaRepository<DatastreamContentDeletionFailure, Long> {

}
