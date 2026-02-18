package org.ddh.gamsapi.domain.DigitalObject.utils.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;
import org.ddh.gamsapi.domain.Datastream.utils.dto.DatastreamMainResourceDto;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.DublinCoreEntryCompactDTO;
import org.ddh.gamsapi.domain.DigitalObject.utils.ArchiveState;
import org.ddh.gamsapi.domain.MetadataBaseEntity;
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

    private Set<String> tags;

    private DatastreamMainResourceDto mainResource;

    private boolean isIngested;

    private ArchiveState archiveState;

    private boolean modifiedAfterCreation;

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
