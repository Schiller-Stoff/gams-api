package org.zim.gamsapi.Datastream;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.*;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.MetadataBaseEntity;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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
@EntityListeners(AuditingEntityListener.class)
@Slf4j
@IdClass(DatastreamId.class)
public class Datastream {


  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @NotNull
  @JoinColumn(nullable = false)
  @Id
  private DigitalObject digitalObject;

  /**
   * Digital object specific identifier for the datastream.
   * Like TEI_SOURCE - MUST be unique per Digital object.
   */
  @Column(name = "dsid")
  @NotEmpty
  @Id
  private String dsid;


  /**
   * Mimetype of the contained data.
   */
  @Column(name = "mime_type")
  @NotEmpty
  private String mimeType;

  @Column(name = "file_name")
  private String fileName;

  @Column
  @NotNull
  // TODO test validation
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
  @NotNull
  @Valid // Add Bean validation to embedded tables
  private MetadataBaseEntity baseMetadata;


  @Column(name = "created_by")
  @CreatedBy
  private String createdBy;


  @Column(name = "modified_by")
  @LastModifiedBy
  private String modifiedBy;

  /**
   * Allows to restrict the content of the datastream to specific users.
   */
  @ElementCollection(fetch = FetchType.EAGER)
  @NotNull
  private Set<String> contentRestrictions = new HashSet<>();


  /**
   * Derives the DatastreamId from the current Datastream object.
   * Follows the pattern of the DatastreamId class which represents the logic stored in the database.
   * @return
   */
  public DatastreamId deriveDatastreamId() {
    if(dsid == null || digitalObject == null) {
      String msg = String.format("Encountered unexpected null value - Tried to derive DatastreamId from Datastream with dsid: %s and digitalObject: %s", dsid, digitalObject);
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    if(dsid.isEmpty() || digitalObject.getId().isEmpty()){
      String msg = String.format("Encountered unexpected empty value - Tried to derive DatastreamId from Datastream with dsid: %s and digitalObject: %s", dsid, digitalObject);
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    return new DatastreamId(dsid, digitalObject.getId());
  }

  /**
   * Two datastreams are considered equal if they have the same digital object and dsid.
   * @param o
   * @return
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    // check hibernate proxy
    Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
    Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
    if (thisEffectiveClass != oEffectiveClass) return false;

    Datastream that = (Datastream) o;
    if(digitalObject == null || that.digitalObject == null) {
      String msg = String.format("Encountered unexpected null value when comparing two digital objects via .equals of a datastream: First %s and that.digitalObject: %s", digitalObject, that.digitalObject);
      log.error(msg);
      throw new IllegalStateException(msg);
      //return false;
    }

    if(dsid == null || that.dsid == null) {
      String msg = String.format("Encountered unexpected null value when comparing two dsids via .equals of a datastream: First %s and that.dsid: %s", dsid, that.dsid);
      log.error(msg);
      throw new IllegalStateException(msg);
      //return false;
    }

    return Objects.equals(digitalObject, that.digitalObject) && Objects.equals(dsid, that.dsid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(digitalObject, dsid);
  }
}
