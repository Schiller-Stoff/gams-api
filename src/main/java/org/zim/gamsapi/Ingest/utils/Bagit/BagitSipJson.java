package org.zim.gamsapi.Ingest.utils.Bagit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.HashSet;
import java.util.Set;

/**
 * Container for the bagit sip.json file defined by invenio / CERN.
 * TODO improve validation e.g add @NotEmpty etc.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BagitSipJson {

    @JsonProperty("recid")
    @NotEmpty
    private String id;

    /**
     * Abbreviation of the GAMS project.
     * TODO missing in test data atm?
     */
    @NotEmpty
    @Size(min = 1, max = 10)
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

    private String parent;

    /**
     * TODO write tests if validation of bagit-content file works as expected!
     */
    private Set<@Valid BagitContentFile> contentFiles = new HashSet<>();

    private Set<String> types = new HashSet<>();

}
