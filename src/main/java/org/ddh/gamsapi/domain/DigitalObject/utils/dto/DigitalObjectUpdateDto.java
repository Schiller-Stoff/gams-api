package org.ddh.gamsapi.domain.DigitalObject.utils.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * DTO for PATCH operations on DigitalObject.
 * Only fields present in the JSON request body will be non-null.
 * Fields omitted from the request remain null and are NOT applied.
 *
 * Important: This DTO intentionally does NOT include Bean Validation
 * annotations like @NotEmpty because null here means "not provided",
 * not "set to empty". Validation happens in the service layer during merge.
 */
@Getter
@Setter
@NoArgsConstructor
public class DigitalObjectUpdateDto {

  // --- MetadataBaseEntity fields (flat for simpler client API) ---
  private String title;
  private String description;
  private String rights;
  private String creator;

  // --- DigitalObject-level fields ---
  private String publisher;
  private String funder;
  private String objectType;
  private Set<String> tags;
}