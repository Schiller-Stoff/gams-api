package org.zim.gamsapi.DigitalObject;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.Project.Project;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Domain object representing a digital object in sense of OAIS.
 */
@Entity
@Table(name = "digital_object")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@Slf4j
public class DigitalObject {

  /**
   * ID of the digital object (= old PID of digital object)
   */
  @Id
  @Column(name = "id")
  @NotBlank
  private String id;

  @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "digitalObject")
  @JsonManagedReference // manages bidirectional reference in json https://www.baeldung.com/jackson-bidirectional-relationships-and-infinite-recursion
  private Set<Datastream> datastreams = new HashSet<>();

  /**
   * A digital object can contain other digital objects.
   */
  @OneToMany
  @NotNull
  // manages bidirectional reference in json https://www.baeldung.com/jackson-bidirectional-relationships-and-infinite-recursion
  @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
  private Set<@NotNull DigitalObject> childObjects = new HashSet<>();

  /**
   * Content Model representation
   */
  @Column(name = "object_type")
  private String objectType;

  /**
   * Date of publication
   */
  @Temporal(TemporalType.TIMESTAMP)
  private Date published;

  /**
   * Creation date of the digital object / datastream
   */
  @Temporal(TemporalType.TIMESTAMP)
  @CreationTimestamp
  private Date created;

  /**
   * Last modified date of the digital object / datatream
   */
  @Temporal(TemporalType.TIMESTAMP)
  @UpdateTimestamp
  private Date modified;

  /**
   * Project to which the digital object belongs to
   */
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

  @Column(name = "created_by")
  @CreatedBy
  private String createdBy;

  @Column(name = "modified_by")
  @LastModifiedBy
  private String modifiedBy;

  /**
   * Arbitrary types associated with the digital object.
   */
  @ElementCollection
  @NotNull
  private Set<String> types = new HashSet<>();

  /**
   * Adds a child digital object to the current digital object.
   * @param datastream Datastream to be added.
   */
  public void addDatastream(Datastream datastream) {
    if (datastream == null) {
      String msg = String.format("Cannot assign a datastream with value null to a digital object %s", this);
      log.error(msg);
      throw new NullPointerException(msg);
    }

    if(datastream.getDigitalObject() != null) {
      String msg = String.format("Datastream %s is already assigned to a digital object. Make sure that no setter is used to assign the datastream to a digital object (in the code before).", datastream);
      log.error(msg);
      throw new IllegalStateException(msg);
    }
    datastreams.add(datastream);
    datastream.setDigitalObject(this);
  }

    /**
     * Removes a child digital object from the current digital object.
     * @param datastream Datastream to be removed
     */
    public void removeDatastream(Datastream datastream) {
        if (datastream == null) {
          String msg = String.format("Cannot remove a datastream with value null from a digital object %s", this);
          log.error(msg);
          throw new NullPointerException("Cannot remove a datastream with the value null.");
        }

        if(datastream.getDigitalObject() == null) {
          String msg = String.format("Datastream %s is not assigned to any digital object and so cannot be removed. Make sure that no setter is used to assign the datastream to a digital object (in the code before).", datastream);
          log.error(msg);
          throw new IllegalArgumentException(msg);
        }
        datastreams.remove(datastream);
        datastream.setDigitalObject(null);
    }

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

  @Override
  public String toString() {

    String childObjectsString = childObjects.stream()
            .map(DigitalObject::getId)
            .collect(Collectors.joining(", "));

    return "DigitalObject{" +
            "id='" + id + '\'' +
            ", datastreams=" + datastreams +
            ", childObjects=[" + childObjectsString + "]" +
            ", objectType='" + objectType + '\'' +
            ", published=" + published +
            ", created=" + created +
            ", modified=" + modified +
            ", project=" + project +
            ", baseMetadata=" + baseMetadata +
            ", createdBy='" + createdBy + '\'' +
            ", modifiedBy='" + modifiedBy + '\'' +
            ", types=" + types +
            '}';
  }

  /**
   * Package private set datastreams to prevent direct manipulation of the datastreams
   * (Hibernate does not allow complete private setters)
   * @param datastreams
   */
  void setDatastreams(Set<Datastream> datastreams) {
    this.datastreams = datastreams;
  }
}
