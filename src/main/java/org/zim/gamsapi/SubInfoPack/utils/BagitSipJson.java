package org.zim.gamsapi.SubInfoPack.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.HashSet;
import java.util.Set;

/**
 * Container for the bagit sip.json file defined by invenio / CERN.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BagitSipJson {

    @JsonProperty("recid")
    @NotNull
    private String id;

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

    private Set<String> childObjects = new HashSet<>();


}
