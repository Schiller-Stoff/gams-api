package org.zim.gamsapi.application.Ingest.utils.Bagit;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import org.zim.gamsapi.application.Ingest.SubmissionRecord;

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

    public static BagMeta from(SubmissionRecord submissionRecord){
        return BagMeta.builder()
                .bagItVersion(submissionRecord.getBagVersion())
                .tagFileCharacterEncoding(submissionRecord.getBagTagFileCharacterEncoding())
                .build();
    }
}
