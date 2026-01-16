package org.ddh.gamsapi.domain.DigitalObject.utils.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Builder;
import lombok.Data;
import org.ddh.gamsapi.domain.Datastream.utils.dto.DatastreamMainResourceDto;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.DublinCoreEntryCompactDTO;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JacksonXmlRootElement(localName = "digitalObject")
public class DigitalObjectSearchResultDTO {
  private String id;
  private String title;
  private String description;
  private String objectType;
  private String projectAbbr;
  private DatastreamMainResourceDto mainResource;
  private Map<String, List<DublinCoreEntryCompactDTO>> dublinCore;
}
