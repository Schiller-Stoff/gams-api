package org.zim.gamsapi.DigitalObject.Ingest.utils.Bagit;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.Ingest.IngestRecord;

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

  public static BagData from(DigitalObject digitalObject, Set<Datastream> datastreams, IngestRecord ingestRecord){

      Set<BagFile> contentFiles = new HashSet<>();
      datastreams.forEach(datastream -> {
          BagFile bagFile = BagFile.from(datastream);
          contentFiles.add(bagFile);
      });

      return BagData.builder()
              .id(digitalObject.getId())
              .project(digitalObject.getProject().getProjectAbbr())
              .title(digitalObject.getBaseMetadata().getTitle())
              .objectType(digitalObject.getObjectType())
              .description(digitalObject.getBaseMetadata().getDescription())
              .creator(digitalObject.getBaseMetadata().getCreator())
              .rights(digitalObject.getBaseMetadata().getRights())
              .publisher(digitalObject.getPublisher())
              .funder(digitalObject.getFunder())
              .mainResource(digitalObject.getMainResource())
              .contentFiles(contentFiles)
              // TODO what is with this types?
              .types(new HashSet<>())
              .md5Checksum(digitalObject.getBaseMetadata().getMd5Checksum())
              .sha512Checksum(digitalObject.getBaseMetadata().getSha512Checksum())
              .schema(ingestRecord.getBagSchema())
              .createdBy(ingestRecord.getBagCreatedBy())
              .source(ingestRecord.getBagSource())
              .build();

  }

}
