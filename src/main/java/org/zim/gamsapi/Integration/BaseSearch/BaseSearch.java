package org.zim.gamsapi.Integration.BaseSearch;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Response of a SOLR document. Used for marshalling and unmarshalling via Jackson.
 */
@Slf4j
@ToString
public class BaseSearch {

  /**
   * JSON Any-Setter: All properties will be mapped to this map.
   * JSONAny-Getter: Keys and values of this map will be dispersed as properties of this
   * class = Facet.java at deserialization.
   */
  @JsonAnySetter
  @JsonAnyGetter
  public final Map<String, Object> properties = new HashMap<>();



}
