package org.ddh.gamsapi.domain.Project.ProjectModification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Helper entity to store the latest modification date of a project.
 * Needed for scalability reasons.
 */
@Getter
@Setter
@ToString
public class ProjectModification {

  /**
   * The project abbreviation.
   */
  @Id
  private String projectAbbr;

  /**
   * The latest modification date of the project.
   */
  //@Column(name = "latest_modification_date")
  private Instant latestModificationDate;

  /**
   * Converts a Date object to a LocalDateTime object using the system default time zone.
   * @return The LocalDateTime object.
   */
  public LocalDateTime getLastModificationDateAsLocalDateTime() {
    return this.latestModificationDate
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime();
  }

}
