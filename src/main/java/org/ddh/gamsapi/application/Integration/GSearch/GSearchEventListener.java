package org.ddh.gamsapi.application.Integration.GSearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.Common.IntegrationFailure;
import org.ddh.gamsapi.application.Integration.Common.enums.IntegrationStatus;
import org.ddh.gamsapi.application.Integration.Common.interfaces.IIntegrationFailureRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.utils.events.DigitalObjectCreatedEvent;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class GSearchEventListener {

  private final GSearchService gSearchService;
  private final IIntegrationFailureRepository failureRepository;

  /**
   * Listen for DigitalObjectCreatedEvent, but only process after the transaction is committed.
   * This ensures that the object is fully persisted before we attempt to index it.
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Async
  public void handleDigitalObjectCreatedEvent(DigitalObjectCreatedEvent event) {

    DigitalObject digitalObject = event.getDigitalObject();

    try {
      gSearchService.indexObject(
          digitalObject.getProject().getProjectAbbr(),
          digitalObject.getId()
      );
    } catch (Exception e) {
      log.error("Error indexing object {}. Storing for retry. Error: {}",
          digitalObject.getId(), e.getMessage());

      // Store failure for retry
      failureRepository.save(
          IntegrationFailure.builder()
              .serviceName("BaseSearch") // TODO better naming / enum?
              .projectAbbr(digitalObject.getProject().getProjectAbbr())
              .digitalObjectId(digitalObject.getId())
              .operation(IntegrationStatus.INDEX) // TODO enum?
              .errorMessage(e.getMessage())
              .nextRetryAt(LocalDateTime.now().plusMinutes(5))
              .build()
      );
    }
  }


}
