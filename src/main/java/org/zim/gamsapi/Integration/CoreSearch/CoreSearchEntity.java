package org.zim.gamsapi.Integration.CoreSearch;

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

  @Id
  private String id;

  @Field(type = FieldType.Text)
  private List<String> titles = new ArrayList<>();

  public void addTitle(String title) {
    if (title != null) {
      this.titles.add(title);
    }
  }

}
