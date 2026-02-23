package org.ddh.gamsapi.domain.Project;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Date;
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
@Slf4j
@ToString
@EntityListeners(AuditingEntityListener.class)
public class Project {

  public static final String ENTITY_TABLE_NAME = "project";

  /**
   * Contains all table names in the order they should be deleted / created.
   */
  public static final String[] ORDERED_MANAGED_TABLES = new String[]{
      ENTITY_TABLE_NAME
  };

  @NotBlank
  @Id
  @Column(name = "project_abbr")
  @Size(min = 2, max = 10)
  @Pattern(regexp = "^[a-z0-9]*$")
  private String projectAbbr;

  /**
   * Title of the GAMS project
   */
  @Column(name = "title", length = 255)
  private String title;

  /**
   * Description of the GAMS project
   */
  @Column(name = "description", length = 5000)
  private String description;

  /**
   * Creation date of the digital object / datastream
   */
  @CreationTimestamp
  private Instant created;

  /**
   * Last modified date of the digital object / datastream
   */
  @UpdateTimestamp
  private Instant modified;

  @Column(name = "created_by")
  @CreatedBy
  private String createdBy;

  @Column(name = "modified_by")
  @LastModifiedBy
  private String modifiedBy;


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
