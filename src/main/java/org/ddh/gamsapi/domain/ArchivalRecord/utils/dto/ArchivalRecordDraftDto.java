package org.ddh.gamsapi.domain.ArchivalRecord.utils.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class ArchivalRecordDraftDto {
  @NotEmpty
  @Length(min=5, max=20)
  public String externalId;
}
