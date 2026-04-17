package org.ddh.gamsapi.domain.Project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight aggregate statistics for a project.
 * All values are computed via COUNT/SUM queries which are efficient
 * with proper indexes on the project foreign key columns.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStatisticsDTO {

  /**
   * Total number of digital objects in this project.
   */
  private long digitalObjectCount;

  /**
   * Total number of datastreams across all digital objects in this project.
   */
  private long datastreamCount;

  /**
   * Total storage size in bytes across all datastreams in this project.
   */
  private long totalStorageBytes;

}