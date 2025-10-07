package org.zim.gamsapi.GAMSCollection;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.Project.Project;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = GAMSCollection.ENTITY_TABLE_NAME)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class GAMSCollection {

  public static final String ENTITY_TABLE_NAME = "collection";
  public static final String DIGITAL_OBJECTS_TABLE_NAME = "collection_digital_object";

  /**
   * Contains all table names in the order they should be deleted / created.
   */
  public static final String[] ORDERED_MANAGED_TABLES = new String[]{
      DIGITAL_OBJECTS_TABLE_NAME,
      ENTITY_TABLE_NAME
  };

  @Id
  @NotEmpty
  @Size(max = 30)
  @Pattern(regexp = "^[a-z0-9.-]*$")
  private String id;

  @NotEmpty
  @Column(name = "title")
  private String title;

  @Size(max = 2000)
  @Column(name = "description")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  @NotNull
  private Project project;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = GAMSCollection.DIGITAL_OBJECTS_TABLE_NAME,
      joinColumns = @JoinColumn(name = "collection_id"),
      inverseJoinColumns = @JoinColumn(name = "digital_object_id")
  )
  private Set<DigitalObject> digitalObjects = new HashSet<>();

  @CreationTimestamp
  private Date created;

  @UpdateTimestamp
  private Date modified;

  @CreatedBy
  private String createdBy;

  @LastModifiedBy
  private String modifiedBy;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GAMSCollection that = (GAMSCollection) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
