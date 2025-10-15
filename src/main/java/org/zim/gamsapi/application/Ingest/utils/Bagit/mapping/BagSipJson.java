package org.zim.gamsapi.application.Ingest.utils.Bagit.mapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * Container for the bag sip.json file defined by CERN.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BagSipJson {

    /**
     * Record ID of the object in GAMS.
     * Corresponds to the digital object id in GAMS.
     */
    @NotEmpty
    private String recid;

    /**
     * Abbreviation of the GAMS project the object belongs to.
     */
    @NotEmpty
    @Size(min = 1, max = 50)
    private String project;

    @NotEmpty
    private String title;

    @NotEmpty
    private String objectType;

    private String description;

    @NotEmpty
    private String creator;

    @NotEmpty
    private String rights;

    @NotEmpty
    private String publisher;

    private String funder;

    private String mainResource;

    private Set<@Valid BagSipJsonContentFile> contentFiles = new HashSet<>();

    private Set<String> types = new HashSet<>();

    @JsonProperty("$schema")
    @NotEmpty
    private String schema;

    @NotEmpty
    private String created_by;

    @NotEmpty
    private String source;

}
