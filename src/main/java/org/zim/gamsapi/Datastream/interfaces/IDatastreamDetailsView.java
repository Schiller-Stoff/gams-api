package org.zim.gamsapi.Datastream.interfaces;

import org.zim.gamsapi.MetadataBaseEntity;

import java.util.Date;

/**
 * Spring Data JPA Projection Interface for a singular Datastream.
 * Represents a DTO for a datastream BUT excluding data from datastreams. (like the binary content)
 * https://thorben-janssen.com/spring-data-jpa-query-projections/
 * (Names of the mapped getter methods must be same as in Datastream)
 */
public interface IDatastreamDetailsView {

    Long getGlobalId();

    DigitalObjectView getDigitalObject();


    String getDsid();

    /**
     * EXCLUDED when fetching datastreams (to decrease load size from the persistence layer)
     * actual binary data
     * @return
     */
   //byte[] getData();

    String getMimeType();

   String getFileName();

    Long getSize();

    String getType();

    Date getCreated();

    Date getModified();


    MetadataBaseEntity getBaseMetadata();


    String getCreatedBy();

    String getModifiedBy();

    interface DigitalObjectView {
        String getId();
    }

}
