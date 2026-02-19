package org.ddh.gamsapi.domain.DigitalObject.utils.events;

import lombok.Getter;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObjectId;
import org.springframework.context.ApplicationEvent;

import java.time.Clock;
import java.util.Date;

/**
 * Represents events related to changing digital objects (PATCH requests / overwriting things via PUT)
 */
@Getter
public class DigitalObjectModifiedEvent extends ApplicationEvent {

  DigitalObjectId objectId;
  Date occurredAt;
  String principal;

  public DigitalObjectModifiedEvent(Object source, DigitalObjectId objectId, Date occurredAt, String principal) {
    super(source);
    this.objectId = objectId;
    this.occurredAt = occurredAt;
    this.principal = principal;
  }

  public DigitalObjectModifiedEvent(Object source, Clock clock,DigitalObjectId objectId, Date occurredAt, String principal) {
    super(source, clock);
    this.objectId = objectId;
    this.occurredAt = occurredAt;
    this.principal = principal;
  }

}
