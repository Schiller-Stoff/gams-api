package org.zim.gamsapi.Integration.CoreSearch;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a digital object written stored via the integrated CoreSearchService.
 */
@Getter
@Setter
@ToString
@Document(indexName = CoreSearchEntity.INDEX_NAME, createIndex = true)
public class CoreSearchEntity {

  /**
   * The name of the index in Elasticsearch where digital objects are stored.
   */
  public static final String INDEX_NAME = "digital_object";

  // TODO add more fields to elastic search

  /**
   * The unique identifier for the digital object.
   * This is used as the document ID in Elasticsearch.
   */
  @Id
  private String id;

  /**
   * Represents title elements from dublin core.
   */
  @Field(type = FieldType.Object)
  private List<DCElement> titles = new ArrayList<>();

  /**
   * Represents description elements from dublin core.
   */
  @Field(type = FieldType.Object)
  private List<DCElement> descriptions = new ArrayList<>();

  public void addTitle(DCElement dcElement) {
    if (dcElement != null) {
      this.titles.add(dcElement);
    }
  }

  public void addDescription(DCElement dcElement) {
    if (dcElement != null) {
      this.descriptions.add(dcElement);
    }
  }

  /*
   * Represents a mapped Dublin Core Element (DCE) with a name, value, and language.
   */
  @Getter
  @Setter
  @Builder
  public static class DCElement {
    String name;
    String value;
    String lang;
  }

}
