package org.zim.gamsapi.Datastream.utils.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Builder;
import lombok.Data;
import java.util.Set;

/**
 * DTO representation of the main resource of a datastream.
 * This is used to provide a compact view of the main resource
 */
@Data
@Builder
@JacksonXmlRootElement(localName = "mainResource")
public class DatastreamMainResourceDto {

  private String dsid;
  private String mimeType;

  private String title;
  private String rights;
  private String creator;
  private String description;

  private Set<String> tags;
  private Set<String> lang;

}
