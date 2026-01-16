package org.ddh.gamsapi.application.Ingest.utils.Bagit;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import org.ddh.gamsapi.domain.DigitalObject.SubmissionRecord.SubmissionRecord;

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


  /**
   * Generates the content of a bagit.txt file from the BagMeta object.
   * @return String representing the content of a bagit.txt file.
   */
  public String toBagItTxtContent(){
      return String.format("BagIt-Version: %s%nTag-File-Character-Encoding: %s%n",
          bagItVersion, tagFileCharacterEncoding);
    }

}
