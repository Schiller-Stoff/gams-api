package org.zim.gamsapi;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
public class MetadataBaseEntity {

  // Note: expected to be managed by parent entity
  //private String dcIdentifier;

  // system controlled
  private List<String> identifier;

  // system controlled
  private List<String> format;

  @NotEmpty
  private List<String> title;

  @NotEmpty
  private List<String> rights;

  @NotEmpty
  private List<String> publisher;

  private List<String> creator;

  @Size(min = 0, max = 2000)
  private String description;

  private List<String> subject;

  private List<String> contributor;

  private List<String> date;

  private List<String> type;

  private List<String> source;

  private List<String> language;

  private List<String> relation;

  private List<String> coverage;

}
