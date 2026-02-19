package org.ddh.gamsapi.domain;

import org.springframework.context.ApplicationEvent;

import java.time.Clock;
import java.util.Date;

/**
 * TODO think about superclass
 */
public abstract class GamsApplicationEvent extends ApplicationEvent {
  String projectAbbr;
  Date occurredAt;
  String principal;

  public GamsApplicationEvent(Object source, String projectAbbr, Date occurredAt, String principal) {
    super(source);
    this.projectAbbr = projectAbbr;
    this.occurredAt = occurredAt;
    this.principal = principal;
  }

  public GamsApplicationEvent(Object source, Clock clock, String projectAbbr, Date occurredAt, String principal) {
    super(source, clock);
    this.projectAbbr = projectAbbr;
    this.occurredAt = occurredAt;
    this.principal = principal;
  }
}
