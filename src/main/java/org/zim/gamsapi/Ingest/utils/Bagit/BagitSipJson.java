package org.zim.gamsapi.Ingest.utils.Bagit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
    @NotNull
    private String id;

    /**
     * Abbreviation of the GAMS project.
     * TODO missing in test data atm?
     */
    @NotEmpty
    @Size(min = 1, max = 10)
    private String project;

    @NotNull
    private String title;

    @NotNull
    private String objectType;

    @NotNull
    private String description;

    @NotNull
    private String creator;

    @NotNull
    private String rights;

    @NotNull
    private String publisher;

    private String parent;

    private Set<BagitContentFile> contentFiles = new HashSet<>();

    private Set<String> types = new HashSet<>();

}
