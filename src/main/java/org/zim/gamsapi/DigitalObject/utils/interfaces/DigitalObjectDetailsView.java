package org.zim.gamsapi.DigitalObject.utils.interfaces;

import org.zim.gamsapi.MetadataBaseEntity;
import java.util.Date;
import java.util.Set;

/**
 * Spring Data JPA Projection Interface for a singular DigitalObject.
 *
 * Represents a DTO for a digital object BUT excluding data from datastreams. (like the binary content)
 *
 * https://thorben-janssen.com/spring-data-jpa-query-projections/
 *
 * (Names of the mapped getter methods must be same as in DigitalObject)
 */
public interface DigitalObjectDetailsView {

    String getId();
    ProjectView getProject();
    String getObjectType();

    MetadataBaseEntity getBaseMetadata();

    Date getCreated();

    Date getPublished();

    Date getModified();

    String getCreatedBy();

    String getModifiedBy();

    String getPublisher();

    String getFunder();

    String getMainResource();

    interface ProjectView {
        String getProjectAbbr();
    }

}
