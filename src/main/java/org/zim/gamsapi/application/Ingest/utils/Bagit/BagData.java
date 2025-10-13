package org.zim.gamsapi.application.Ingest.utils.Bagit;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
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
   * Generates the content of a sip.json file from the BagData object.
   * @return String representing the content of a sip.json file.
   */
  public String toSipJsonContent(){

    // Build sip.json from digital object metadata
    Map<String, Object> sipJson = new LinkedHashMap<>();
    sipJson.put("recid", this.getId());
    sipJson.put("project", this.getProject());
    sipJson.put("title", this.getTitle());
    sipJson.put("objectType", this.getObjectType());
    sipJson.put("description", this.getDescription());
    sipJson.put("creator", this.getCreator());
    sipJson.put("rights", this.getRights());
    sipJson.put("publisher", this.getPublisher());

    if (this.getFunder() != null) {
      sipJson.put("funder", this.getFunder());
    }

    if (this.getMainResource() != null) {
      sipJson.put("mainResource", this.getMainResource());
    }

    List<Map<String, Object>> contentFiles = this.getContentFiles().stream().map(contentFile -> {
      Map<String, Object> fileMap = new LinkedHashMap<>();
      fileMap.put("dsid", contentFile.getDsid());
      fileMap.put("filename", contentFile.getBagpath());
      fileMap.put("mimetype", contentFile.getMimetype());
      fileMap.put("title", contentFile.getTitle());
      fileMap.put("description", contentFile.getDescription());
      fileMap.put("creator", contentFile.getCreator());
      fileMap.put("rights", contentFile.getRights());
      fileMap.put("size", contentFile.getSize());
      fileMap.put("tags", new ArrayList<>(contentFile.getTags()));
      fileMap.put("lang", new ArrayList<>(contentFile.getLang()));
      return fileMap;
    }).collect(Collectors.toList());


    sipJson.put("contentFiles", contentFiles);
    sipJson.put("$schema", this.getSchema());
    sipJson.put("created_by", this.getCreatedBy());
    sipJson.put("source", this.getSource());

    // Convert to JSON
    // TODO use jackson instead?

    return toJson(sipJson);

  }


  /**
   * TODO jdoc
   * @param map
   * @return
   */
  private String toJson(Map<String, Object> map) {
    // Simple JSON serialization - you should use Jackson in production
    StringBuilder json = new StringBuilder("{\n");
    Iterator<Map.Entry<String, Object>> iter = map.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<String, Object> entry = iter.next();
      json.append("  \"").append(entry.getKey()).append("\": ");
      json.append(toJsonValue(entry.getValue()));
      if (iter.hasNext()) {
        json.append(",");
      }
      json.append("\n");
    }
    json.append("}");
    return json.toString();
  }

  /**
   * TODO jdoc
   * @param value
   * @return
   */
  @SuppressWarnings("unchecked")
  private String toJsonValue(Object value) {
    if (value == null) {
      return "null";
    } else if (value instanceof String) {
      return "\"" + escapeJson((String) value) + "\"";
    } else if (value instanceof Number) {
      return value.toString();
    } else if (value instanceof List) {
      List<?> list = (List<?>) value;
      return "[" + list.stream()
          .map(this::toJsonValue)
          .collect(Collectors.joining(", ")) + "]";
    } else if (value instanceof Map) {
      return toJson((Map<String, Object>) value);
    }
    return "\"" + value.toString() + "\"";
  }

  /**
   * TODO jdoc
   * @param str
   * @return
   */
  private String escapeJson(String str) {
    return str.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }


}
