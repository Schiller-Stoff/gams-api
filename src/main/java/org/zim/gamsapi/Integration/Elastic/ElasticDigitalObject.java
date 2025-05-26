package org.zim.gamsapi.Integration.Elastic;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Getter
@Setter
@Document(indexName = "digitalobject", createIndex = true)
public class ElasticDigitalObject {

  // TODO add more fields to elastic search

  @Id
  private String id;

}
