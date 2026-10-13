package org.ddh.gamsapi.domain.DigitalObject.utils.interfaces;

import java.time.Instant;

/**
 * Spring data jpa projection meant to carry only minimal auditing data.
 */
public interface DigitalObjectAuditingView {

  String getId();
  Instant getCreated();
  Instant getModified();

}
