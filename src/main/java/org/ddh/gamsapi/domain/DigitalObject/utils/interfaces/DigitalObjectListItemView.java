package org.ddh.gamsapi.domain.DigitalObject.utils.interfaces;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.ddh.gamsapi.domain.DigitalObject.utils.ArchiveState;

import java.util.Set;

/**
 * Spring Data JPA Projection Interface for DigitalObject as list item.
 *
 * Represents a DTO for a digital object as list item e.g. needed
 * when listing all digital objects of a project - BUT excluding some data e.g. datastreams.
 *
 * https://thorben-janssen.com/spring-data-jpa-query-projections/
 *
 * (Names of the mapped getter methods must be same as in DigitalObject)
 */
@JacksonXmlRootElement(localName = "digitalObject")
public interface DigitalObjectListItemView {

    String getId();
    ProjectView getProject();
    String getObjectType();

    BaseMetadataView getBaseMetadata();

    String getCreated();

    String getModified();

    Set<String> getTags();

    Boolean isIngested();

    ArchiveState getArchiveState();

    boolean isModifiedAfterCreation();

    interface ProjectView {
        String getProjectAbbr();
    }

    interface BaseMetadataView {
        String getTitle();
        String getDescription();
    }

}
