package org.zim.gamsapi.Ingest.utils.Bagit;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import org.zim.gamsapi.Ingest.IngestRecord;

/**
 * Metadata obtained from bagit.txt file.
 * Bag version + encoding of tag files.
 */
@Builder
@Getter
public class BagMeta {
    @NotEmpty
    @Size(min = 1, max = 100)
    private String bagItVersion;
    @NotEmpty
    public String tagFileCharacterEncoding;

    public static BagMeta from(IngestRecord ingestRecord){
        return BagMeta.builder()
                .bagItVersion(ingestRecord.getBagVersion())
                .tagFileCharacterEncoding(ingestRecord.getBagTagFileCharacterEncoding())
                .build();
    }
}
