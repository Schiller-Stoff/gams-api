package org.zim.gamsapi.domain.DigitalObject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
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
import org.springframework.http.HttpStatus;
import org.zim.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectException;
import org.zim.gamsapi.domain.MetadataBaseEntity;
import org.zim.gamsapi.domain.Project.Project;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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
public class DigitalObject {

  public static final String ENTITY_TABLE_NAME = "digital_object";

  /**
   * Contains all table names in the order they should be deleted / created.
   */
  public static final String[] ORDERED_MANAGED_TABLES = new String[]{
      ENTITY_TABLE_NAME,
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

  @AssertTrue(message = "The id of a digital object must be valid, i.e. start with the project abbreviation followed by a dot.")
  public boolean isValidDigitalObjectId() {

    List<String> validationViolations = new ArrayList<>();


    if(id == null || project == null) {
      String msg = String.format("Digital object id or project is null. Digital object: %s", this);
      log.error(msg);
      throw new DigitalObjectException(HttpStatus.CONFLICT, msg);
    }

    int minLength = 5;
    if(id.length() < minLength){
      String msg = String.format("Digital object id is too short (shorter than %s). Got id: %s",minLength,  this.getId());
      validationViolations.add(msg);
    }

    int maxLength = 64;
    if(id.length() > maxLength){
      String msg = String.format("Digital object id is too long (bigger than %s). Got id: %s", maxLength, this.getId());
      validationViolations.add(msg);
    }


    // e.g. o:derla.sty256
    // o: -> type prefix
    // derla -> project abbreviation
    // sty256 -> local identifier

    String projectAbbrWithLocalId;

    // type prefix is optional, so we strip it if present
    if(id.contains(":")){
      projectAbbrWithLocalId = id.substring(id.indexOf(":") + 1);
    } else {
      projectAbbrWithLocalId = id;
    }

    // first check if the id (with removed type prefix) starts with the expected project abbreviation and dot
    String expectedStartsWith = project.getProjectAbbr() + ".";
    if(!projectAbbrWithLocalId.startsWith(expectedStartsWith)){
      String msg = String.format("Digital object id does not start with the expected project abbreviation. Expected to start with: %s - but got: %s",
          expectedStartsWith, projectAbbrWithLocalId);
      validationViolations.add(msg);
    }

    String projectAbbr;

    // extract at first occurring dot
    projectAbbr = projectAbbrWithLocalId.substring(0, projectAbbrWithLocalId.indexOf('.'));

    // validate that extracted project abbr matches the expected project abbr
    if(!projectAbbr.equals(project.getProjectAbbr())){
      String msg = String.format("Extracted project abbreviation does not match the expected project abbreviation. Expected projectAbbr: %s - but extracted: %s. Current id: %s",
          this.getProject().getProjectAbbr(), projectAbbr, this.getId());
      validationViolations.add(msg);
    }

    String dotExpected = projectAbbrWithLocalId.substring(projectAbbr.length(), projectAbbr.length() + 1);
    if(!dotExpected.equals(".")){
       String msg = String.format("Digital object id does not contain a dot (.) after the project abbreviation. Analyzed part of the object id: %s Got the string %s instead of dot. Digital object id: %s", projectAbbrWithLocalId, dotExpected, this.getId());
      validationViolations.add(msg);
    }

    // TODO projectAbbr pattern?

    String localId = projectAbbrWithLocalId.substring(projectAbbr.length() + 1); // +1 to also skip the dot

    if (localId.contains("..")) {
      String msg = String.format("Digital object id contains consecutive dots. Digital object id: %s", this.getId());
      validationViolations.add(msg);
    }

    if(localId.contains("_")){
      String msg = String.format("Digital object id contains underscores. Digital object id: %s", this.getId());
      validationViolations.add(msg);
    }

    if(localId.contains("--")){
      String msg = String.format("Digital object id contains consecutive dashes. Digital object id: %s", this.getId());
      validationViolations.add(msg);
    }

    String patternString = "^[a-z0-9][a-z0-9.-]*$";
    // validate this regex: ^[a-z0-9.]*$
    if(!localId.matches(patternString)){
      String msg = String.format("Digital object id contains invalid characters but MUST follow the regex: %s. Tested local id part: %s Digital object id: %s", patternString, localId, this.getId());
      validationViolations.add(msg);
    }

    if(validationViolations.isEmpty()){
      return true;
    } else {
      String msg = String.format("Digital object id %s is not valid. Encountered %s violation(s). %s. For digital object: %s", this.getId(), validationViolations.size(), validationViolations, this);
      log.error(msg);
      throw new DigitalObjectException(HttpStatus.CONFLICT, msg);
    }

  }

}
