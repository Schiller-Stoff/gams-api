package org.zim.gamsapi.Ingest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = BagEntity.ENTITY_TABLE_NAME)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Slf4j
@ToString(exclude = "digitalObject") // Prevent circular reference in toString
public class BagEntity {

    public static final String ENTITY_TABLE_NAME = "bag_entity";

    public static final String[] ORDERED_MANAGED_TABLES = new String[]{
            ENTITY_TABLE_NAME
    };

    /**
     * Shared primary key with DigitalObject.
     * MapsId indicates this ID comes from the digitalObject relationship.
     */
    @Id
    @Column(name = "digital_object_id")
    private String id;

    /**
     * OneToOne relationship to DigitalObject.
     * means the PK of this entity is derived from digitalObject.id
     * optional=false ensures referential integrity
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId // This is the KEY annotation for shared PK
    @JoinColumn(name = "digital_object_id")
    @NotNull
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private DigitalObject digitalObject;

    @Column
    @NotEmpty
    private String schema;

    @Column
    @NotEmpty
    private String createdBy;

    @Column
    @NotEmpty
    private String source;

    @Column(name = "bagging_timestamp")
    @NotNull
    private Instant baggingTimeStamp;

    @Column
    @NotEmpty
    @Email
    private String contactMail;

    @Column
    @NotEmpty
    private String externalDescription;

    @Column
    @NotNull
    @Min(1)
    private Float payloadOxum;


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
        BagEntity that = (BagEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }

    public static org.zim.gamsapi.Ingest.BagEntityBuilder builder(){
        return new org.zim.gamsapi.Ingest.BagEntityBuilder();
    }

}
