package org.zim.gamsapi.SubInfoPack.utils.Bagit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class BagitContentFile {

    /**
     * Size of the file in bytes.
     */
    @NotNull
    private long size;

    /**
     * Path to the file in the bagit archive.
     */
    @NotEmpty
    private String bagpath;

    /**
     * Id of the datastream to be created
     */
    @NotEmpty
    private String dsid;


    /**
     * Mimetype of the file.
     */
    @NotEmpty
    private String mimetype;


    /**
     * Title of the datastream
     */
    @NotEmpty
    private String title;


    /**
     * Description of the datastream
     */
    @NotEmpty
    private String description;


    /**
     * Creator of the datastream
     */
    @NotEmpty
    private String creator;


    /**
     * Rights statement of the datastream
     */
    @NotEmpty
    private String rights;


    /**
     * Publisher of the datastream
     */
    @NotEmpty
    private String publisher;

}
