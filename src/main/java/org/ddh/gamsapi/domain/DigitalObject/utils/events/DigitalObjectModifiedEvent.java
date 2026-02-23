package org.ddh.gamsapi.domain.DigitalObject.utils.events;

import lombok.Getter;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObjectId;
import org.ddh.gamsapi.domain.GamsApplicationEvents;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;

/**
 * Represents events related to changing digital objects (PATCH requests / overwriting things via PUT)
 */
@Getter
public class DigitalObjectModifiedEvent extends GamsApplicationEvents {

  public DigitalObjectModifiedEvent(Object source, DigitalObjectId objectId, Instant occurredAt, String principal) {
    super(source, objectId, occurredAt, principal);
  }

  public DigitalObjectModifiedEvent(Object source, Clock clock, DigitalObjectId objectId, Instant occurredAt, String principal) {
    super(source, clock, objectId, occurredAt, principal);
  }
}
