package org.ddh.gamsapi.application.Ingest.utils.Bagit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Ingest.exceptions.ExportProcessingException;
import org.ddh.gamsapi.domain.Datastream.Datastream;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.SubmissionRecord.SubmissionRecord;

import java.util.HashSet;
import java.util.Set;

/**
 * Container for the data defined related to bagit sip.json file defined by invenio / CERN.
 * With additional fields for checksums calculated during bag read.
 */
@Data
@Builder
@Slf4j
public class BagData {

  @NotEmpty
  @JsonProperty("recid")
  private String id;

  /**
   * Abbreviation of the GAMS project.
   */
  @NotEmpty
  @Size(min = 1, max = 10)
  @JsonProperty("project")
  private String project;

  @NotEmpty
  @JsonProperty("title")
  private String title;

  @NotEmpty
  @JsonProperty("objectType")
  private String objectType;

  @JsonProperty("description")
  private String description;

  @NotEmpty
  @JsonProperty("creator")
  private String creator;

  @NotEmpty
  @JsonProperty("rights")
  private String rights;

  @NotEmpty
  @JsonProperty("publisher")
  private String publisher;

  @JsonProperty("funder")
  private String funder;

  @JsonProperty("mainResource")
  private String mainResource;

  /**
   * Tags associated with the digital object.
   */
  @JsonProperty("tags")
  private Set<String> tags;

  @JsonProperty("contentFiles")
  private Set<BagFile> contentFiles = new HashSet<>();

  @NotEmpty
  // WRITE_ONLY means: can be deserialized FROM JSON, but NOT serialized TO JSON
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String md5Checksum;

  @NotEmpty
  // WRITE_ONLY means: can be deserialized FROM JSON, but NOT serialized TO JSON
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String sha512Checksum;

  @NotEmpty
  @JsonProperty("$schema")
  private String schema;

  @NotEmpty
  @JsonProperty("created_by")
  private String createdBy;

  @NotEmpty
  @JsonProperty("source")
  private String source;

  public static BagData from(DigitalObject digitalObject, Set<Datastream> datastreams,
                             SubmissionRecord submissionRecord) {

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
        .tags(digitalObject.getTags())
        .contentFiles(contentFiles)
        .md5Checksum("") // placeholder — calculated during export
        .sha512Checksum("") // placeholder — calculated during export
        .schema(submissionRecord.getBagSchema())
        .createdBy(submissionRecord.getBagCreatedBy())
        .source(submissionRecord.getBagSource())
        .build();
  }

  /**
   * Creates and configures an ObjectMapper for sip.json serialization.
   * Configuration matches the expected sip.json format.
   *
   * @return configured ObjectMapper instance
   */
  private ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();

    // Pretty printing for human-readable output (CERN format uses indentation)
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    // Don't include null values (aligns with your manual implementation)
    // This is already handled by @JsonInclude if needed, but can be set globally
    mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);

    // Ensure consistent ordering (though @JsonPropertyOrder handles this)
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, false);

    return mapper;
  }


  /**
   * Generates the content of a sip.json file from the BagData object.
   * @return String representing the content of a sip.json file.
   */
  public String toSipJsonContent(){
    try {
      ObjectMapper objectMapper = createObjectMapper();
      return objectMapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new ExportProcessingException(
          "Error creating Sip JSON content for BagData related to digital object " + this.id + " Original error: " + e.getMessage(),
          e);
    }
  }

}
