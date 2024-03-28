package org.zim.gamsapi.Project;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class Project {

  @NotBlank
  @Id
  @Column(name = "project_abbr")
  private String projectAbbr;

  @OneToMany(cascade = {CascadeType.REMOVE, CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true, mappedBy = "project")
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

  /**
   * Adds a digital object to the project.
   * @param digitalObject the digital object to add
   */
  public void addDigitalObject(DigitalObject digitalObject) {
    if(digitalObject == null){
      String msg = String.format("Cannot assign a digital object with value null to the project %s", this);
      log.error(msg);
      throw new NullPointerException(msg);
    }

    if(digitalObject.getProject() != null){
      String msg = String.format("Digital object %s is already assigned to a project.", digitalObject);
      log.error(msg);
      throw new IllegalArgumentException(msg);
    }

    digitalObjects.add(digitalObject);
    digitalObject.setProject(this);
  }

  /**
   * Removes a digital object from the project.
   * @param digitalObject the digital object to remove
   */
  public void removeDigitalObject(DigitalObject digitalObject) {
    if(digitalObject == null){
      String msg = String.format("Cannot remove a digital object with value null from the project %s", this);
      log.error(msg);
      throw new NullPointerException(msg);
    }

    if(digitalObject.getProject() == null){
      String msg = String.format("Digital object %s is not assigned to a project.", digitalObject);
      log.error(msg);
      throw new IllegalArgumentException(msg);
    }

    digitalObjects.remove(digitalObject);
    digitalObject.setProject(null);
  }

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

  void setDigitalObjects(Set<DigitalObject> digitalObjects) {
    this.digitalObjects = digitalObjects;
  }
}
