package org.ddh.gamsapi.domain.ArchivalRecord;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState;
import org.hibernate.validator.constraints.Length;

import java.time.Instant;

/**
 * DTO for creating an ArchivalRecord (represents user requests)
 */
@Data
public class ArchivalRecordDto {
  @ValidPid
  private String pid;

  private String objectId;

  private Instant publicationTimeStamp;

  private String externalId;

  @NotNull
  private ArchivalState archivalState;

}
