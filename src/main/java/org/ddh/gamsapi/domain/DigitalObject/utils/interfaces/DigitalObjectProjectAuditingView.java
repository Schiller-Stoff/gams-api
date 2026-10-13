package org.ddh.gamsapi.domain.DigitalObject.utils.interfaces;

import java.time.Instant;

public interface DigitalObjectProjectAuditingView {

  String getId();
  ProjectView getProject();
  Instant getModified();

  interface ProjectView {
    String getProjectAbbr();
  }

}
