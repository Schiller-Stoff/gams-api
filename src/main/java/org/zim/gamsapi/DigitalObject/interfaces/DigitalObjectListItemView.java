package org.zim.gamsapi.DigitalObject.interfaces;

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
public interface DigitalObjectListItemView {

    String getId();
    ProjectView getProject();
    String getObjectType();

    BaseMetadataView getBaseMetadata();

    String getCreated();

    Set<String> getTypes();

    interface ProjectView {
        String getProjectAbbr();
    }

    interface BaseMetadataView {
        String getTitle();
        String getDescription();
    }

}
