package org.ddh.gamsapi.domain;

import lombok.Getter;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObjectId;
import org.springframework.context.ApplicationEvent;

import java.time.Clock;
import java.util.Date;

/**
 * Superclass for CRUD operations using the domain layer.
 */
@Getter
public abstract class GamsApplicationEvents extends ApplicationEvent {

  DigitalObjectId objectId;
  Date occurredAt;
  String principal;

  public GamsApplicationEvents(Object source, DigitalObjectId objectId, Date occurredAt, String principal) {
    this.objectId = objectId;
    this.occurredAt = occurredAt;
    this.principal = principal;
    super(source);
  }

  public GamsApplicationEvents(Object source, Clock clock, DigitalObjectId objectId, Date occurredAt, String principal) {
    this.objectId = objectId;
    this.occurredAt = occurredAt;
    this.principal = principal;
    super(source, clock);
  }
}
