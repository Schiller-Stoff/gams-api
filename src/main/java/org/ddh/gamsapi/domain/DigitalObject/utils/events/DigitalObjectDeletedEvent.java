package org.ddh.gamsapi.domain.DigitalObject.utils.events;

import lombok.Getter;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObjectId;
import org.springframework.context.ApplicationEvent;

import java.time.Clock;
import java.util.Date;

/**
 * Represents deletion events of digital objects
 * TODO might have the same base class as DigitalObjectModifiedEvent?
 */
@Getter
public class DigitalObjectDeletedEvent extends ApplicationEvent {

  DigitalObjectId objectId;
  Date occurredAt;
  String principal;

  public DigitalObjectDeletedEvent(Object source, DigitalObjectId objectId, Date occurredAt, String principal) {
    this.objectId = objectId;
    this.occurredAt = occurredAt;
    this.principal = principal;
    super(source);
  }

  public DigitalObjectDeletedEvent(Object source, Clock clock, DigitalObjectId objectId, Date occurredAt, String principal) {
    this.objectId = objectId;
    this.occurredAt = occurredAt;
    this.principal = principal;
    super(source, clock);
  }
}
