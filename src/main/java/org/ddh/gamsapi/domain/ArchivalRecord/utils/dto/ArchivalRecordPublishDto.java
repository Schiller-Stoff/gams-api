package org.ddh.gamsapi.domain.ArchivalRecord.utils.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class ArchivalRecordPublishDto {
  @NotNull
  public Instant publicationTimeStamp;
}
