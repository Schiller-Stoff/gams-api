package org.zim.gamsapi;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.util.List;

/**
 * Adds missing DC metadata to the entities of gams-api BUT:
 * dc:identifier AND dc:title are managed by the parent entities
 *
 * https://www.dublincore.org/
 */
@Embeddable
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DCBaseEntity {

  // Note: expected to be managed by parent entity
  //private String dcIdentifier;

  private String title;

  private List<String> creator;

  private String description;

  private List<String> subject;

  private List<String> publisher;

  private List<String> contributor;

  private String date;

  private List<String> type;

  private String format;

  private String source;

  private String language;

  private List<String> relation;

  private List<String> coverage;

  private List<String> rights;


}
