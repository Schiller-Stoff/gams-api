package org.ddh.gamsapi.domain.ArchivalRecord;

import lombok.Data;

/**
 * DTO for changing an archival record
 */
@Data
public class ArchivalRecordUpdateDto {
  private String objectId;
  private String publicationTimeStamp;
  private String externalId;
}
