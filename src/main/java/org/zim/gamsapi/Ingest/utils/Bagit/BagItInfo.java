package org.zim.gamsapi.Ingest.utils.Bagit;

import lombok.Builder;
import lombok.Data;

/**
 * Represents the key value pairs in the bag-info.txt file.
 */
@Data
@Builder
public class BagItInfo {

  private String date;
  private String time;
  private String payloadOxum;
  private String contactMail;
  private String externalDescription;
}
