package org.ddh.gamsapi.domain.ArchivalRecord.utils.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.Instant;

@Data
public class ArchivalRecordPublishDto {
  @NotNull
  @PastOrPresent
  public Instant publicationTimeStamp;
}
