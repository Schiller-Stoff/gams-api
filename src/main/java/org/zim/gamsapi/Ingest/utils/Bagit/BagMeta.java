package org.zim.gamsapi.Ingest.utils.Bagit;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

/**
 * Metadata obtained from bagit.txt file.
 * Bag version + encoding of tag files.
 */
@Builder
@Getter
public class BagMeta {
    @NotEmpty
    @Size(min = 1, max = 100)
    private String  bagItVersion;
    @NotEmpty
    public  String tagFileCharacterEncoding;
}
