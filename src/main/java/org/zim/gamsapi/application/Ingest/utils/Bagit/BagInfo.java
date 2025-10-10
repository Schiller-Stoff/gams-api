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
    @NotEmpty
    @Size(min = 12, max = 12)
  private String time;
  @NotNull
  @Min(1)
  private Float payloadOxum;
  @NotEmpty
  @Email
  private String contactMail;
  @NotEmpty
  @Size(min = 5)
  private String externalDescription;

    /**
     * Parses the date and time fields of the bag-info.txt and returns an Instant representing the bagging timestamp.
     * @return Instant representing the bagging timestamp.
     */
  public Instant getBaggingTimeStamp() {

      final String utcTimeZoneString = " UTC";

      if(!time.contains(utcTimeZoneString)){
          String msg = String.format("The time field in bag-info.txt does not contain the expected timezone information (%s). Actual value: %s", utcTimeZoneString, time);
          log.error(msg);
          throw new IngestProcessingException(msg);
      }

      String timeWithoutZone = time.replace(utcTimeZoneString, "");
      return LocalDateTime
              .of(LocalDate.parse(date), LocalTime.parse(timeWithoutZone))
              .toInstant(ZoneOffset.UTC);
  }

  public static BagInfo from(SubmissionRecord submissionRecord) {
      return BagInfo.builder()
              .date(submissionRecord.getBaggingDate())
              .time(submissionRecord.getBaggingTime())
              // TODO this needs to be regenerated
              .payloadOxum(submissionRecord.getBagPayloadOxum())
              .contactMail(submissionRecord.getBagContactMail())
              .externalDescription(submissionRecord.getBagExternalDescription())
              .build();
  }

}
