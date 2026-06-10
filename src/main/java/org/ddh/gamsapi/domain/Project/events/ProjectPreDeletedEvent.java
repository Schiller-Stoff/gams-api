package org.ddh.gamsapi.domain.Project.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.time.Instant;

/**
 * Event represents the state directly before deletion.
 * (e.g. needed to first delete related web deployments)
 */
@Getter
public class ProjectPreDeletedEvent extends ApplicationEvent {

  private final String projectAbbr;
  private final Instant occurredAt;
  private final String principal;

  public ProjectPreDeletedEvent(Object source, String projectAbbr, Instant occurredAt, String principal) {
    super(source);
    this.projectAbbr = projectAbbr;
    this.occurredAt = occurredAt;
    this.principal = principal;
  }

}
