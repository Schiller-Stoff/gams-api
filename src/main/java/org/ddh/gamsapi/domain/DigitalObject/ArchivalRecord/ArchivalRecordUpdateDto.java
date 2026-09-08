package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import lombok.Data;

/**
 * DTO for updated an archival record (represents user requests)
 */
@Data
public class ArchivalRecordUpdateDto {
  private Long archivalRecordId;
  private String pid;
  private String externalId;
}
