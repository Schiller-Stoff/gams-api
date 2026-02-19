package org.ddh.gamsapi.domain.DigitalObject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.DigitalObject.utils.ArchiveState;
import org.ddh.gamsapi.domain.DigitalObject.utils.validation.ValidDigitalObjectId;
import org.ddh.gamsapi.domain.MetadataBaseEntity;
import org.ddh.gamsapi.domain.Project.Project;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GeneratedColumn;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Domain object representing a digital object in sense of OAIS.
 */
@Entity
@Table(name = DigitalObject.ENTITY_TABLE_NAME)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@Slf4j
@ToString
@JacksonXmlRootElement(localName = "digitalObject")
@ValidDigitalObjectId
public class DigitalObject {

  public static final String ENTITY_TABLE_NAME = "digital_object";
  public static final String TAGS_TABLE_NAME = ENTITY_TABLE_NAME + "_tags";

  /**
   * Contains all table names in the order they should be deleted / created.
   */
  public static final String[] ORDERED_MANAGED_TABLES = new String[]{
      ENTITY_TABLE_NAME,
      TAGS_TABLE_NAME
  };

  /**
   * ID of the digital object (= old PID of digital object)
   */
  @Id
  @Column(name = "id")
  @NotEmpty
  private String id;

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
   * Last modified date of the digital object / datastream
   */
  @Temporal(TemporalType.TIMESTAMP)
  @UpdateTimestamp
  private Date modified;

  /**
   * Project to which the digital object belongs to
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  @NotNull
  // this will fix some serialization issues with Hibernate proxies
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
  private Project project;

  @Embedded
  // increases allowed length of description based on the EmbeddedEntity
  @AttributeOverride(name = "description", column = @Column(length = 2000))
  @Valid  // Add Bean validation to embedded tables
  @NotNull
  private MetadataBaseEntity baseMetadata;

  @Column(name = "created_by")
  @CreatedBy
  private String createdBy;

  @Column(name = "modified_by")
  @LastModifiedBy
  private String modifiedBy;

  /**
   * Publisher of the digital object
   */
  @Column(name = "publisher")
  @NotEmpty
  private String publisher;

  /**
   * Funder of the digital object
   */
  @Column(name = "funder")
  private String funder;

  /**
   * Main resource of the digital object
   */
  @Column(name = "main_resource")
  private String mainResource;

  /**
   * Tags for a digital object.
   */
  @ElementCollection(fetch = FetchType.EAGER)
  @NotNull
  @Column(name = TAGS_TABLE_NAME)
  @Size(max = 100, message = "Maximum 100 tags allowed per digital object")
  private Set<String> tags = new HashSet<>();

  /**
   * Tracks if the object was created via ingest.
   * Immutable history track.
   */
  @Column(name = "ingested", nullable = false, updatable = false)
  private boolean ingested = false;

  /**
   * Tracks if an object was moved to an archive / repository.
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ArchiveState archiveState = ArchiveState.NOT_ARCHIVED;

  /**
   * Describes if a digital object was changed after it's creation
   * (Handled by by the application - not meant to be set by users).
   */
  @Column(name = "modified_after_creation")
  private boolean modifiedAfterCreation = false;

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
    DigitalObject digitalObject = (DigitalObject) o;
    return getId() != null && Objects.equals(getId(), digitalObject.getId());
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
