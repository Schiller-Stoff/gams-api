package org.zim.gamsapi.domain.DigitalObject.SubmissionRecord;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.zim.gamsapi.domain.DigitalObject.DigitalObject;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = SubmissionRecord.ENTITY_TABLE_NAME)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Slf4j
@ToString(exclude = "digitalObject") // Prevent circular reference in toString
public class SubmissionRecord {

    public static final String ENTITY_TABLE_NAME = "submission_record";

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
    @JsonIgnore
    private DigitalObject digitalObject;

    /**
     * Schema used by the bag
     */
    @Column
    @NotEmpty
    private String bagSchema;

    /**
     * The user / program that created the bag.
     */
    @Column
    @NotEmpty
    private String bagCreatedBy;

    /**
     * The source system or application that produced the bag.
     */
    @Column
    @NotEmpty
    private String bagSource;

    /**
     * Date of the bag creation.
     */
    @Column
    @NotNull
    private String baggingDate;

    /**
     * Contact email for the bag creator or responsible party.
     */
    @Column
    @NotEmpty
    @Email
    private String bagContactMail;

    /**
     * Description of the bag's contents or purpose.
     */
    @Column
    @NotEmpty
    private String bagExternalDescription;

    /**
     * Total size of the payload in bytes.
     */
    @Column
    @NotNull
    @NotEmpty
    private String bagPayloadOxum;

    /**
     * Version of the BagIt specification used.
     */
    @Column
    @NotEmpty
    private String bagVersion;

    /**
     * Character encoding used in tag files.
     */
    @Column
    @NotEmpty
    private String bagTagFileCharacterEncoding;

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
        SubmissionRecord that = (SubmissionRecord) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }

    public static SubmissionRecordBuilder builder(){
        return new SubmissionRecordBuilder();
    }


}
