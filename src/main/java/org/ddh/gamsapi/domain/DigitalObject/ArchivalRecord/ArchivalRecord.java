package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = ArchivalRecord.ENTITY_TABLE_NAME)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Slf4j
@ToString(exclude = "digitalObject") // Prevent circular reference in toString
public class ArchivalRecord {

  public static final String ENTITY_TABLE_NAME = "archival_record";

  protected static final String[] ORDERED_MANAGED_TABLES = new String[]{
      ENTITY_TABLE_NAME
  };

  /**
   * Generated unique identifier for the dublin core element
   * (unique for the full gams-api).
   */
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  /**
   * Digital object the dublin core archival record belongs to.
   */
  @ManyToOne(fetch = FetchType.LAZY) // fetchType lazy = means that the digital object is loaded only when accessed
  @JoinColumn(name = "digital_object_id", nullable = false)
  @NotNull
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
  @JsonIgnore
  private DigitalObject digitalObject;

  @Column(name = "pid", nullable = false)
  @NotEmpty
  private String pid;

  @Column(name = "timestamp", nullable = false)
  @NotNull
  private Instant timeStamp;

  @Column(name = "archival_status")
  @Enumerated(EnumType.STRING)
  @NotNull
  private ArchivingStatus archivingStatus = ArchivingStatus.DRAFTED;

  @Column(name = "external_id", nullable = false)
  @NotNull
  private String externalId;

  /**
   * Proper equals/hashCode for entities with assigned IDs.
   * Based on the pattern you use in your codebase.
   */
  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    Class<?> oEffectiveClass = o instanceof HibernateProxy
        ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
        : o.getClass();
    Class<?> thisEffectiveClass = this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
        : this.getClass();
    if (thisEffectiveClass != oEffectiveClass) return false;
    ArchivalRecord that = (ArchivalRecord) o;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
        : getClass().hashCode();
  }

}
