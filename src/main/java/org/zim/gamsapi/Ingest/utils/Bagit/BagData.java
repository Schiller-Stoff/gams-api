package org.zim.gamsapi.Ingest.utils.Bagit;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import java.util.HashSet;
import java.util.Set;

/**
 * Container for the data defined related to bagit sip.json file defined by invenio / CERN.
 * With additional fields for checksums calculated during bag read.
 */
@Data
@Builder
public class BagData {

  @NotEmpty
  private String id;

  /**
   * Abbreviation of the GAMS project.
   */
  @NotEmpty
  @Size(min = 1, max = 10)
  private String project;

  @NotEmpty
  private String title;

  @NotEmpty
  private String objectType;

  private String description;

  @NotEmpty
  private String creator;

  @NotEmpty
  private String rights;

  @NotEmpty
  private String publisher;

  private String funder;

  private String mainResource;

  private Set<BagFile> contentFiles = new HashSet<>();

  private Set<String> types = new HashSet<>();

  @NotEmpty
  private String md5Checksum;

  @NotEmpty
  private String sha512Checksum;

  @NotEmpty
  private String schema;

  @NotEmpty
  private String createdBy;

  @NotEmpty
  private String source;

}
