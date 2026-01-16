package org.ddh.gamsapi.domain.Datastream.utils.interfaces;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.ddh.gamsapi.domain.MetadataBaseEntity;
import java.util.Date;
import java.util.Set;

/**
 * Spring Data JPA Projection Interface for a singular Datastream.
 * Represents a DTO for a datastream BUT excluding data from datastreams. (like the binary content)
 * https://thorben-janssen.com/spring-data-jpa-query-projections/
 * (Names of the mapped getter methods must be same as in Datastream)
 */
@JacksonXmlRootElement(localName = "datastream")
public interface IDatastreamDetailsView {


    DigitalObjectView getDigitalObject();


    String getDsid();

    String getMimeType();

   String getFileName();

    Long getSize();

    String getType();

    Date getCreated();

    Date getModified();


    MetadataBaseEntity getBaseMetadata();


    String getCreatedBy();

    String getModifiedBy();

    Set<String> getTags();

    Set<String> getLang();

    interface DigitalObjectView {
        String getId();
    }

}
