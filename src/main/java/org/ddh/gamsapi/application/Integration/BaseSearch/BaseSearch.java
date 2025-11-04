package org.ddh.gamsapi.application.Integration.BaseSearch;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrDocument;

import java.util.HashMap;
import java.util.Map;

/**
 * Part of the response
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


  public void  addProperty(String key, Object value) {
    properties.put(key, value);
  }

  public void removeProperty(String key) {
    properties.remove(key);
  }

  public Object getProperty(String key) {
    return properties.get(key);
  }

  /**
   * Creates BaseSearch instance from SolrDocument.
   * @param solrDocument SolrDocument
   * @return BaseSearch instance
   */
  public static BaseSearch from(SolrDocument solrDocument) {

    BaseSearch baseSearch = new BaseSearch();
    solrDocument.getProperties().forEach((key, value) -> {
      baseSearch.addProperty(key, value);
    });

    return baseSearch;

  }



}
