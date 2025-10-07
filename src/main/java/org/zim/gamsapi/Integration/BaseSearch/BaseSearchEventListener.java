package org.zim.gamsapi.Integration.BaseSearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DigitalObjectCreatedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class BaseSearchEventListener {

  private final BaseSearchService baseSearchService;

  /**
   * Listen for DigitalObjectCreatedEvent, but only process after the transaction is committed.
   * This ensures that the object is fully persisted before we attempt to index it.
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Async
  public void handleDigitalObjectCreatedEvent(DigitalObjectCreatedEvent event) {

    DigitalObject digitalObject = event.getDigitalObject();
    log.debug("Handling DigitalObjectCreatedEvent for object: {}", digitalObject.getId());

    try {
      baseSearchService.indexObject(
          digitalObject.getProject().getProjectAbbr(),
          digitalObject.getId()
      );
    } catch (Exception e) {
      log.error("Error processing DigitalObjectCreatedEvent for object: {}. Error: {}", digitalObject.getId(), e.getMessage());
      return;
    }



  }


}
