package org.zim.gamsapi.DigitalObject.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Builder;
import lombok.Data;
import org.zim.gamsapi.Datastream.dto.DatastreamMainResourceDto;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamDetailsView;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamMainResourceView;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntryCompactDTO;

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
