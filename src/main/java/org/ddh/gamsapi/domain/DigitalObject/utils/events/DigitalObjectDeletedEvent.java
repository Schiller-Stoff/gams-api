package org.ddh.gamsapi.domain.DigitalObject.utils.events;

import lombok.Getter;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObjectId;
import org.ddh.gamsapi.domain.GamsApplicationEvents;

import java.time.Clock;
import java.util.Date;

/**
 * Represents deletion events of digital objects
 *
 */
@Getter
public class DigitalObjectDeletedEvent extends GamsApplicationEvents {

  public DigitalObjectDeletedEvent(Object source, DigitalObjectId objectId, Date occurredAt, String principal) {
    super(source, objectId, occurredAt, principal);
  }

  public DigitalObjectDeletedEvent(Object source, Clock clock, DigitalObjectId objectId, Date occurredAt, String principal) {
    super(source, clock, objectId, occurredAt, principal);
  }
}
