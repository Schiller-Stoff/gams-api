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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  @Id
  private String id;

  @Field(type = FieldType.Object)
  private List<DCELement> titles = new ArrayList<>();

  public void addTitle(DCELement dcElement) {
    if (dcElement != null) {
      this.titles.add(dcElement);
    }
  }

  @Getter
  @Setter
  @Builder
  public static class DCELement {
    String name;
    String value;
    String lang;
  }

}
