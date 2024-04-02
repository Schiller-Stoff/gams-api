package org.zim.gamsapi.Datastream;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.MetadataBaseEntity;
import java.util.Date;
import java.util.Objects;

/**
 * Domain class for datastream domain objects.
 */
@Entity
@Table(
        name = "datastream",
        // this will guarantee that the dsid field is unique per referenced digital object!
        uniqueConstraints = {@UniqueConstraint(name = "DatastreamNameUniquePerObject", columnNames = { "digital_object_id", "dsid" })})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Slf4j
public class Datastream {

  /**
   * Global id of datastream - each datastream has an unique identifier
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "global_id")
  private Long globalId;

  @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
  @ToString.Exclude
  // manages bidirectional reference in json https://www.baeldung.com/jackson-bidirectional-relationships-and-infinite-recursion
  @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
  @JsonIdentityReference(alwaysAsId = true)
  @NotNull
  private DigitalObject digitalObject;

  /**
   * Digital object specific identifier for the datastream.
   * Like TEI_SOURCE - MUST be unique per Digital object.
   */
  @Column(name = "dsid")
  @NotEmpty
  private String dsid;

  /**
   * Actual contained binary data
   */
  @Column(name = "data")
  @ToString.Exclude
  @JsonIgnore
  private byte[] data;

  /**
   * Mimetype of the contained data.
   */
  @Column(name = "mime_type")
  private String mimeType;

  @Column(name = "file_name")
  private String fileName;

  @Column
  private Long size;

  @Column
  private String type;

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


  @Embedded
  // increases allowed length of description based on the EmbeddedEntity
  @AttributeOverride(name = "description", column = @Column(length = 2000))
  private MetadataBaseEntity baseMetadata;


  @Column(name = "created_by")
  @CreatedBy
  private String createdBy;


  @Column(name = "modified_by")
  @LastModifiedBy
  private String modifiedBy;

  /**
   * Makes sure that the bidirectional relationship is removed before deleting from the database.
   * https://stackoverflow.com/questions/22688402/delete-not-working-with-jparepository
   */
  @PreRemove
  private void removeDigitalObjectFromDatastream() {
    if (digitalObject != null) {
      digitalObject.removeDatastream(this);
    }
  }

  /**
   * equals and hashCode for JPA entities with DB-generated IDs
   * https://jpa-buddy.com/blog/hopefully-the-final-article-about-equals-and-hashcode-for-jpa-entities-with-db-generated-ids/
   */
  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
    Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
    if (thisEffectiveClass != oEffectiveClass) return false;
    Datastream datastream = (Datastream) o;
    return getGlobalId() != null && Objects.equals(getGlobalId(), datastream.getGlobalId());
  }

  /**
   * equals and hashCode for JPA entities with DB-generated IDs
   * https://jpa-buddy.com/blog/hopefully-the-final-article-about-equals-and-hashcode-for-jpa-entities-with-db-generated-ids/
   */
  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
        : getClass().hashCode();
  }

}
