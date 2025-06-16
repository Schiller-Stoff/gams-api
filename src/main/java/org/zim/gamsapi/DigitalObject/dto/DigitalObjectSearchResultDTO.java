package org.zim.gamsapi.DigitalObject.dto;

import lombok.Builder;
import lombok.Data;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntryCompactDTO;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DigitalObjectSearchResultDTO {
  private String id;
  private String title;
  private String description;
  private String objectType;
  private String projectAbbr;
  private String mainResource;
  private Map<String, List<DublinCoreEntryCompactDTO>> dublinCore;
}
