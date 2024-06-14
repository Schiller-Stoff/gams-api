package org.zim.gamsapi.Project;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import java.util.Objects;

/**
 * Class representing a GAMS project.
 */
@Getter
@Setter
@Table(name = "project")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Slf4j
public class Project {

  @NotBlank
  @Id
  @Column(name = "project_abbr")
  private String projectAbbr;

  @Column(name = "description")
  private String description;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Project project = (Project) o;
    return Objects.equals(projectAbbr, project.projectAbbr);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectAbbr);
  }


}
