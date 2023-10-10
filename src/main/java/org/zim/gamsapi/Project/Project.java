package org.zim.gamsapi.Project;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.zim.gamsapi.DigitalObject.DigitalObject;

import java.util.List;

/**
 * Class representing a GAMS project.
 */
@Data
@Table(name = "project")
@Entity
@AllArgsConstructor
@NoArgsConstructor
//@Builder
public class Project {

  @NotBlank
  @Id
  @Column(name = "project_abbr")
  private String projectAbbr;

  @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "project")
  //@Builder.Default
  @JsonManagedReference
  private List<DigitalObject> digitalObjects;

  @Column(name = "description")
  private String description;

}
