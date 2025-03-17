package org.zim.gamsapi.DigitalObject.DigitalObjectModification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Helper entity to store the latest modification date of a project.
 * Needed for scalability reasons.
 */
// next lines are commented out because the entity is not used in the current version of the application
// but might be useful in future scaling of the application
//@Entity
//@Table(name = "digital_object_modification_",
//    indexes = @Index(columnList = "latest_modification_date"))
@Getter
@Setter
@ToString
public class DigitalObjectModification {

  @Id
  private String id;

  @Column(name = "latest_modification_date")
  private Date latestModificationDate;


  /**
   * Converts a Date object to a LocalDateTime object using the system default time zone.
   * @return The LocalDateTime object.
   */
  public LocalDateTime getLastModificationDateAsLocalDateTime() {
    return this.latestModificationDate.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime();
  }
}
