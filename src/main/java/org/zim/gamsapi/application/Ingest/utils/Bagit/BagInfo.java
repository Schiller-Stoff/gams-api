package org.zim.gamsapi.application.Ingest.utils.Bagit;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.zim.gamsapi.domain.DigitalObject.SubmissionRecord.SubmissionRecord;
import org.zim.gamsapi.application.Ingest.exceptions.IngestProcessingException;

import java.time.*;

/**
 * Represents the key value pairs in the bag-info.txt file.
 */
@Slf4j
@Data
@Builder
public class BagInfo {
    @NotEmpty
    @Size(min = 10, max = 10)
  private String date;
  @NotNull
  @NotEmpty
  private String payloadOxum;
  @NotEmpty
  @Email
  private String contactMail;
  @NotEmpty
  @Size(min = 5)
  private String externalDescription;


  public static BagInfo from(SubmissionRecord submissionRecord) {
      return BagInfo.builder()
              .date(submissionRecord.getBaggingDate().toString())
              .payloadOxum(submissionRecord.getBagPayloadOxum())
              .contactMail(submissionRecord.getBagContactMail())
              .externalDescription(submissionRecord.getBagExternalDescription())
              .build();
  }

  /**
   * Generates the content of the bag-info.txt file based on the fields of this BagInfo object.
   * @param payloadOxum the payload oxum to use in the bag-info content
   * @return content of bag-info.txt
   */
  public String toBagInfoContent(String payloadOxum){
    return String.format(
        "Bagging-Date: %s%n" +
            "Contact-Email: %s%n" +
            "External-Description: %s%n" +
            "Payload-Oxum: %s%n",
        date,
        contactMail,
        externalDescription,
        payloadOxum
    );
  }

  /**
   * Generates the content of the bag-info.txt file based on the fields of this BagInfo object.
   * @return content of bag-info.txt
   */
  public String toBagInfoContent(){
    return toBagInfoContent(this.payloadOxum);
  }

}
