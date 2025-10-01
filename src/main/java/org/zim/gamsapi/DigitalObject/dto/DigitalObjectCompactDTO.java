package org.zim.gamsapi.DigitalObject.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;
import org.zim.gamsapi.Datastream.dto.DatastreamMainResourceDto;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntryCompactDTO;
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
@ToString
@JacksonXmlRootElement(localName = "digitalObject")
public class DigitalObjectCompactDTO {

    private String id;

    private String objectType;

    private Date published;

    private Date created;

    private Date modified;

    private String projectAbbr;

    private MetadataBaseEntity baseMetadata;

    private String createdBy;

    private String modifiedBy;

    private String publisher;

    private String funder;

    private DatastreamMainResourceDto mainResource;

    private String bagSchema;

    private String bagSource;

    private String bagCreatedBy;

    /**
     * Map of Dublin Core entries, where the key is the name of the entry and the value is a list of
     * DublinCoreEntrySummaryView objects.
     */
    private Map<String, List<DublinCoreEntryCompactDTO>> dublinCore = new HashMap<>();

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

}
