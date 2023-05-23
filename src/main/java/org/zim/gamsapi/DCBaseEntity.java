package org.zim.gamsapi;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

  @NotEmpty
  @NotNull
  private List<String> identifier;

  @NotEmpty
  @NotNull
  private List<String> title;

  @NotEmpty
  private List<String> rights;

  @NotNull
  @NotEmpty
  private List<String> format;

  @NotEmpty
  @NotNull
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
