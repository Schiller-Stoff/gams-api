package org.zim.gamsapi.Integration.Common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import org.zim.gamsapi.Integration.Common.enums.IntegrationActionStatus;
import org.zim.gamsapi.Integration.Common.enums.IntegrationActionType;

/**
 * Basic report for indexing status.
 * Meant as a response to the client.
 */
@Data
@ToString
@AllArgsConstructor
public class IntegrationActionReport {
  private String projectAbbr;
  private IntegrationActionType type;
  private IntegrationActionStatus status;
  private String message;
}
