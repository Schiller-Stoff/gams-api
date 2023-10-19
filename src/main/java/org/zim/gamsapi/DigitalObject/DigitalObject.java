package org.zim.gamsapi.DigitalObject;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.Project.Project;

import java.util.*;

/**
 * Domain object representing a digital object in sense of OAIS.
 */
@Entity
@Table(name = "digital_object")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class DigitalObject {

  /**
   * ID of the digital object (= old PID of digital object)
   */
  @Id
  @Column(name = "id")
  @NotBlank
  private String id;

  // TODO datatype should be a set instead of list (same datastream cannot be contained twice)
  @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "digitalObject")
  @Builder.Default
  @JsonManagedReference // manages bidirectional reference in json https://www.baeldung.com/jackson-bidirectional-relationships-and-infinite-recursion
  private List<Datastream> datastreams = new ArrayList<>();

  /**
   * A digital object can contain other digital objects.
   */
  @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
  @NotNull
  private Set<@NotNull DigitalObject> childObjects = new HashSet<>();

  /**
   * Content Model representation
   */
  @Column(name = "object_type")
  private String objectType;


  @ManyToOne
  @JsonBackReference
  @NotNull
  // manages bidirectional reference in json https://www.baeldung.com/jackson-bidirectional-relationships-and-infinite-recursion
  private Project project;

  @Embedded
  // increases allowed length of description based on the EmbeddedEntity
  @AttributeOverride(name = "description", column = @Column(length = 2000))
  @Valid  // Add Bean validation to embedded tables
  private MetadataBaseEntity baseMetadata;

  /**
   * Creation date of the digital object
   */
  @Temporal(TemporalType.TIMESTAMP)
  @CreationTimestamp
  private Date created;

  /**
   * Last modified date of the digital object
   */
  @Temporal(TemporalType.TIMESTAMP)
  @UpdateTimestamp
  private Date modified;

  /**
   * Date of publication
   */
  @Temporal(TemporalType.TIMESTAMP)
  private Date published;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DigitalObject that = (DigitalObject) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
