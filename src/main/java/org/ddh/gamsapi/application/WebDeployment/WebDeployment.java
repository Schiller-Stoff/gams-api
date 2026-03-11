package org.ddh.gamsapi.application.WebDeployment;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

/**
 * Tracks metadata for a project's static web deployment.
 * One deployment per project (upserted on each PUT).
 */
@Entity
@Table(name = WebDeployment.ENTITY_TABLE_NAME)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class WebDeployment {

  public static final String ENTITY_TABLE_NAME = "web_deployment";

  @Id
  @Column(name = "project_abbr", length = 10)
  @NotBlank
  private String projectAbbr;

  @Column(name = "deployed_at", nullable = false)
  @NotNull
  private Instant deployedAt;

  @Column(name = "deployed_by", nullable = false)
  @NotBlank
  private String deployedBy;

  @Column(name = "file_count", nullable = false)
  @PositiveOrZero
  private int fileCount;

  @Column(name = "total_size", nullable = false)
  @PositiveOrZero
  private long totalSize;
}