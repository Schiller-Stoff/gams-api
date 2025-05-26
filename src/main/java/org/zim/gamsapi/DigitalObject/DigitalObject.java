package org.zim.gamsapi.DigitalObject;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.http.HttpStatus;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectException;
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
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@Slf4j
@ToString
public class DigitalObject {

  /**
   * ID of the digital object (= old PID of digital object)
   */
  @Id
  @Column(name = "id")
  @Size(max = 30, min = 1)
  @NotEmpty
  @Pattern(regexp = "^[a-z0-9.]*$")
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

  @AssertTrue(message = "The id of a digital object must start with the project abbreviation followed by dot, like 'hsa.1234'")
  public boolean isCorrectlyContainingProjectAbbrInIdWithDot() {
    if(id == null || project == null) {
      String msg = String.format("Digital object id or project is null. Digital object: %s", this);
      log.error(msg);
      throw new DigitalObjectException(HttpStatus.CONFLICT, msg);
    }
    return id.startsWith(project.getProjectAbbr() + ".");
  }

}
