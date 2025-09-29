package org.zim.gamsapi.Ingest.utils.Bagit;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

/**
 * Represents the key value pairs in the bag-info.txt file.
 */
@Data
@Builder
public class BagInfo {
    @NotEmpty
    @Size(min = 10, max = 10)
  private String date;
    @NotEmpty
    @Size(min = 12, max = 12)
  private String time;
  @NotEmpty
  @Size(min = 5)
  private String payloadOxum;
  @NotEmpty
  @Email
  private String contactMail;
  @NotEmpty
  @Size(min = 5)
  private String externalDescription;
}
