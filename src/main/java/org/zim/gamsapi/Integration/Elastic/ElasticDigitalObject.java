package org.zim.gamsapi.Integration.Elastic;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Getter
@Setter
@Document(indexName = ElasticDigitalObject.INDEX_NAME, createIndex = true)
public class ElasticDigitalObject {

  /**
   * The name of the index in Elasticsearch where digital objects are stored.
   */
  public static final String INDEX_NAME = "digital_object";

  // TODO add more fields to elastic search

  @Id
  private String id;

}
