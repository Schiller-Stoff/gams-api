package org.ddh.gamsapi.domain.DigitalObject.utils.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DigitalObjectCreateDto {

  /**
   * The suffix part of the ID (will be prepended with projectAbbr + ".")
   */
  @NotEmpty
  @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "ID may only contain letters, numbers, dots, hyphens, underscores")
  @Size(max = 200)
  private String idSuffix;

  @NotEmpty
  @Size(max = 255)
  private String title;

  @NotEmpty
  @Size(max = 255)
  private String creator;

  @NotEmpty
  @Size(max = 255)
  private String rights;

  @NotEmpty
  @Size(max = 255)
  private String publisher;

  @Size(max = 2000)
  private String description;

  @Size(max = 255)
  private String objectType;

  @Size(max = 255)
  private String funder;
}