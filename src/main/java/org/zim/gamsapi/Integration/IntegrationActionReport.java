package org.zim.gamsapi.Integration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/**
 * Basic report for indexing status.
 * Meant as a response to the client.
 */
@Data
@ToString
@AllArgsConstructor
public class IntegrationActionReport {
  private String projectAbbr;
  private String indexingStatus;
  private String indexingMessage;
}
