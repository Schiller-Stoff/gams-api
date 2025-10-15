package org.ddh.gamsapi.domain.Datastream.DatastreamContent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.ddh.gamsapi.domain.Datastream.DatastreamId;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatastreamContentDeletionFailureCleanupJob {

  private final DatastreamContentDeletionFailureRepository datastreamContentDeletionFailureRepository;
  private final IDatastreamContentRepository datastreamContentRepository;

  /**
   * Scheduled job to process failed datastream content deletions.
   */
  @Scheduled(fixedRate = 3000000) // Every 50 minutes
  public void processFailedDeletions(){
    log.info("*** Datastream content cleanup job: Starting job for failed datastream content deletions.");

    var failedDeletions = datastreamContentDeletionFailureRepository.findAll();

    if(failedDeletions.isEmpty()){
      log.info("*** Datastream content cleanup job: No failed deletions to process.");
      return;
    }

    failedDeletions.forEach(failedDeletion -> {
      try {
        datastreamContentRepository.delete(
            DatastreamId.builder()
                .digitalObject(failedDeletion.getDigitalObjectId())
                .dsid(failedDeletion.getDatastreamDsid())
                .build()
        );
        datastreamContentDeletionFailureRepository.delete(failedDeletion);
        log.info("*** Datastream content cleanup job: Successfully deleted datastream's content file digital object {} and datastream {}.",
            failedDeletion.getDigitalObjectId(), failedDeletion.getDatastreamDsid());
      } catch (Exception e) {
        String msg = String.format("*** Datastream content cleanup job: Failed to delete datastream's content file digital object %s and datastream %s. Error: %s"
            , failedDeletion.getDigitalObjectId(), failedDeletion.getDatastreamDsid(), e.getMessage());
        log.error(msg,e);
        // document number of retries
        failedDeletion.setRetryCount(failedDeletion.getRetryCount() + 1);
        datastreamContentDeletionFailureRepository.save(failedDeletion);
      }
    });

  }

}
