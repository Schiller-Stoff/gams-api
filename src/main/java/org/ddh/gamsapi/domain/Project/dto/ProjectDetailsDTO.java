package org.ddh.gamsapi.domain.Project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for the project detail endpoint.
 * Combines project metadata with lightweight aggregate statistics.
 * <p>
 * This is the JSON response shape for {@code GET /api/v1/projects/{projectAbbr}}.
 * It intentionally does NOT include lists of digital objects or datastreams —
 * those are served by their own paginated endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDetailsDTO {

  private String projectAbbr;

  private String title;

  private String description;

  private Instant created;

  private Instant modified;

  private String createdBy;

  private String modifiedBy;

  private ProjectStatisticsDTO statistics;

}