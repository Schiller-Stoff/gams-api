package org.ddh.gamsapi.domain.Datastream.utils.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ddh.gamsapi.domain.Datastream.utils.ArchivalPolicy;

import java.util.Set;

/**
 * DTO for PATCH operations on Datastream.
 * Only fields present in the JSON request body will be non-null.
 * Fields omitted from the request remain null and are NOT applied.
 *
 * <p>Important: This DTO intentionally does NOT include Bean Validation
 * annotations like {@code @NotEmpty} because null here means "not provided",
 * not "set to empty". Validation happens in the service layer after merge
 * via {@code validateInvariants()}.</p>
 *
 * <p>The {@code dsid} and {@code digitalObject} fields are deliberately excluded
 * because they form the composite primary key and must never be changed.
 * Similarly, content-derived fields (size, checksums, mimeType, bagPath) are
 * only changed when content is updated via file upload, not through this DTO.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class DatastreamUpdateDto {

  // --- MetadataBaseEntity fields (flat for simpler client API) ---

  private String title;

  @Size(max = 2000, message = "Description must not exceed 2000 characters")
  private String description;

  private String creator;

  private String rights;

  // --- Datastream-level fields ---

  private Set<String> tags;

  private Set<String> lang;

  /**
   * Comma-separated tags string from form submission.
   * Parsed into the tags Set in the controller/service layer.
   * The JSON API should use the 'tags' field directly.
   */
  private String tagsCommaSeparated;

  /**
   * Comma-separated lang string from form submission.
   * Parsed into the lang Set in the controller/service layer.
   * The JSON API should use the 'lang' field directly.
   */
  private String langCommaSeparated;

  private ArchivalPolicy archivalPolicy;
}