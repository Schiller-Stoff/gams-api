package org.zim.gamsapi.application.Ingest.utils.Bagit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.zim.gamsapi.application.Ingest.exceptions.ExportProcessingException;
import org.zim.gamsapi.domain.Datastream.Datastream;
import org.zim.gamsapi.domain.DigitalObject.DigitalObject;
import org.zim.gamsapi.domain.DigitalObject.SubmissionRecord.SubmissionRecord;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Container for the data defined related to bagit sip.json file defined by invenio / CERN.
 * With additional fields for checksums calculated during bag read.
 */
@Data
@Builder
@Slf4j
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
  // WRITE_ONLY means: can be deserialized FROM JSON, but NOT serialized TO JSON
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String md5Checksum;

  @NotEmpty
  // WRITE_ONLY means: can be deserialized FROM JSON, but NOT serialized TO JSON
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String sha512Checksum;

  @NotEmpty
  private String schema;

  @NotEmpty
  private String createdBy;

  @NotEmpty
  private String source;

  public static BagData from(DigitalObject digitalObject, Set<Datastream> datastreams, SubmissionRecord submissionRecord){

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
      String msg = String.format("Error creating Sip JSON content for BagData related to digital object %s. Original error: %s", this.id, e);
      log.error(msg);
      throw new ExportProcessingException(msg);
    }
  }

}
