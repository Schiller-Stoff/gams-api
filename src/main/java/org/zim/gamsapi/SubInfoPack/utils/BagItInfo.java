package org.zim.gamsapi.SubInfoPack.utils;

import lombok.Builder;
import lombok.Data;

/**
 * Represents the key value pairs in the bag-info.txt file.
 */
@Data
@Builder
public class BagItInfo {

  private String id;
  private String title;
  private String contactMail;
  private String type;

}
