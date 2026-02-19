package org.ddh.gamsapi.domain.DigitalObject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.DigitalObject.utils.events.DigitalObjectDeletedEvent;
import org.ddh.gamsapi.domain.DigitalObject.utils.events.DigitalObjectModifiedEvent;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectNotFoundException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class DigitalObjectEventListener {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final IProjectRepository projectRepository;


  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  public void handleDigitalObjectModifiedEvent(DigitalObjectModifiedEvent digitalObjectModifiedEvent) {

    // update digital object and project
    var foundObject = digitalObjectRepository.findById(digitalObjectModifiedEvent.getObjectId().getId())
        .orElseThrow(() -> new DigitalObjectNotFoundException("Cannot update modified digital object because it was not found " + digitalObjectModifiedEvent.getObjectId().getId()));

    foundObject.setModified(digitalObjectModifiedEvent.getOccurredAt());
    foundObject.setModifiedBy(digitalObjectModifiedEvent.getPrincipal());
    foundObject.setModifiedAfterCreation(true);

    var foundProject = projectRepository.findById(digitalObjectModifiedEvent.getObjectId().deriveProjectAbbr())
        .orElseThrow(() -> new ProjectNotFoundException("Cannot update modified digital object because it's parent project was not found " + digitalObjectModifiedEvent.getObjectId().getId()));

    foundProject.setModified(digitalObjectModifiedEvent.getOccurredAt());
    foundProject.setModifiedBy(digitalObjectModifiedEvent.getPrincipal());

  }

  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  public void handleDigitalObjectDeletedEvent(DigitalObjectDeletedEvent digitalObjectDeletedEvent) {

    var foundProject = projectRepository.findById(digitalObjectDeletedEvent.getObjectId().deriveProjectAbbr())
        .orElseThrow(() -> new ProjectNotFoundException("Cannot update modified project because it was not found: " + digitalObjectDeletedEvent.getObjectId().deriveProjectAbbr() + " For obejct: " + digitalObjectDeletedEvent.getObjectId()));

    foundProject.setModified(digitalObjectDeletedEvent.getOccurredAt());
    foundProject.setModifiedBy(digitalObjectDeletedEvent.getPrincipal());

    // TODO remove this field? replace through boolean / harmonize with DigitalObject!
    foundProject.setContentLastModified(digitalObjectDeletedEvent.getOccurredAt());


  }

}
