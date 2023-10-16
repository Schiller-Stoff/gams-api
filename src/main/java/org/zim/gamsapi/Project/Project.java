package org.zim.gamsapi.Project;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.User.User;

import java.util.List;
import java.util.Set;

/**
 * Class representing a GAMS project.
 */
@Data
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

  @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "project")
  //@Builder.Default
  @JsonManagedReference
  @ToString.Exclude
  private List<DigitalObject> digitalObjects;

  @Column(name = "description")
  private String description;

  @ManyToMany(mappedBy = "projects")
  @JsonManagedReference // manages bidirectional reference in json https://www.baeldung.com/jackson-bidirectional-relationships-and-infinite-recursion
  @ToString.Exclude
  private Set<User> users;

}
