package org.zim.gamsapi.Project;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.User.User;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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
public class Project {

  @NotBlank
  @Id
  @Column(name = "project_abbr")
  private String projectAbbr;

  @OneToMany(cascade = {CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "project")
  @Builder.Default
  @JsonManagedReference
  @ToString.Exclude
  private Set<DigitalObject> digitalObjects = new HashSet<>();

  @Column(name = "description")
  private String description;

  @ManyToMany(mappedBy = "projects")
  @JsonIgnore
  // manages bidirectional reference in json https://www.baeldung.com/jackson-bidirectional-relationships-and-infinite-recursion
  private Set<User> users;

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
