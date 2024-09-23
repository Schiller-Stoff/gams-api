package org.zim.gamsapi.DigitalObject;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.zim.gamsapi.MetadataBaseEntity;
import java.util.*;

/**
 * DTO representation of a DigitalObject with reduced querying requirements, like datastreams and childobjects are
 * just list of ids.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DigitalObjectCompactDTO {

    private String id;

    /**
     * List of datastream ids
     * TODO this seems wrong here?
     */
    @Builder.Default
    private List<String> datastreams = new ArrayList<>();

    private String objectType;

    private Date published;

    private Date created;

    private Date modified;

    private String projectAbbr;

    private MetadataBaseEntity baseMetadata;

    private String createdBy;

    private String modifiedBy;

    private String publisher;

    @Builder.Default
    private Set<String> types = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DigitalObjectCompactDTO that = (DigitalObjectCompactDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {

        return "DigitalObjectCompactDTO{" +
                "id='" + id + '\'' +
                ", datastreams=" + datastreams +
                ", objectType='" + objectType + '\'' +
                ", published=" + published +
                ", created=" + created +
                ", modified=" + modified +
                ", projectAbbr=" + projectAbbr +
                ", baseMetadata=" + baseMetadata +
                ", createdBy='" + createdBy + '\'' +
                ", modifiedBy='" + modifiedBy + '\'' +
                ", publisher='" + publisher + '\'' +
                ", types=" + types +
                '}';
    }

}
