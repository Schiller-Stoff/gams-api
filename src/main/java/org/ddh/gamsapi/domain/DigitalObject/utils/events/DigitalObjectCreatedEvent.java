package org.ddh.gamsapi.domain.DigitalObject.utils.events;

import lombok.Getter;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObjectId;
import org.ddh.gamsapi.domain.GamsApplicationEvents;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;

/**
 * Spring event that is published when a new DigitalObject is created.
 */
@Getter
public class DigitalObjectCreatedEvent extends GamsApplicationEvents {

  /**
   * During creation the digital object should be available.
   */
  DigitalObject digitalObject;

  public DigitalObjectCreatedEvent(Object source, DigitalObjectId objectId, Instant occurredAt, String principal, DigitalObject digitalObject) {
    this.digitalObject = digitalObject;
    super(source, objectId, occurredAt, principal);
  }

  public DigitalObjectCreatedEvent(Object source, Clock clock, DigitalObjectId objectId, Instant occurredAt, String principal) {
    super(source, clock, objectId, occurredAt, principal);
  }
}
