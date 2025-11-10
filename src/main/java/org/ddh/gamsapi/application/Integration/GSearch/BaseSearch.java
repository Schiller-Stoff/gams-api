package org.ddh.gamsapi.application.Integration.GSearch;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrDocument;
import org.springframework.core.io.InputStreamResource;

import java.io.IOException;
import java.io.InputStream;
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
    solrDocument.retrievePropertiesMap().forEach((key, value) -> {
      baseSearch.addProperty(key, value);
    });

    return baseSearch;

  }

  /**
   * TODO move objectMapper -> own SolrDocumentMapper component
   * TODO test
   * TODO jdoc
   * @param solrDocumentAsResource
   * @return
   */
  public static BaseSearch[] from(InputStreamResource solrDocumentAsResource) throws IOException {

    JsonFactory jsonFactory = new JsonFactory();

    try (
        InputStream inputStream = solrDocumentAsResource.getInputStream();
        JsonParser parser = jsonFactory.createParser(inputStream)
    ){
      //01. parse as BaseSearch list
      var OBJECT_MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();
      OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      BaseSearch[] baseSearch = OBJECT_MAPPER.readValue(parser, BaseSearch[].class);
      return  baseSearch;
    } catch (IOException e) {
      log.error("Error parsing SolrDocument JSON array stream", e);
      throw e;
    }

  }



}
