package org.ddh.gamsapi.domain.Datastream.utils.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for creating a new datastream via direct file upload.
 * Represents the minimal metadata the user must provide.
 * Checksums and file size are computed server-side.
 */
@Getter
@Setter
@NoArgsConstructor
public class DatastreamCreateDto {

  @NotEmpty(message = "Title is required")
  @Size(max = 255)
  private String title;

  @Size(min = 2, max = 2000, message = "Description must be between 2 and 2000 characters")
  private String description;

  @NotEmpty(message = "Creator is required")
  @Size(max = 255)
  private String creator;

  @NotEmpty(message = "Rights statement is required")
  @Size(max = 255)
  private String rights;
}