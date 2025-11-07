package org.ddh.gamsapi.application.Integration.Common.utils.solr;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Response of a SOLR document. Used for marshalling and unmarshalling via Jackson.
 */
@Slf4j
@ToString
public class SolrDocument {

  /**
   * JSON Any-Setter: All properties will be mapped to this map.
   * JSONAny-Getter: Keys and values of this map will be dispersed as properties of this
   * class = Facet.java at deserialization.
   */
  @JsonAnySetter
  @JsonAnyGetter
  public final Map<String, Object> properties = new HashMap<>();


  public void addProperty(String key, Object value) {
    properties.put(key, value);
  }

  public void removeProperty(String key) {
    properties.remove(key);
  }

  public Object getProperty(String key) {
    return properties.get(key);
  }

  /**
   * Careful - returns the internal map reference!
   * @return
   */
  public Map<String, Object> retrievePropertiesMap() {
    return properties;
  }



  /**
   * TODO move objectMapper -> own SolrDocumentMapper component
   * TODO test
   * TODO jdoc
   * @param solrDocumentAsResource
   * @return
   */
  public static SolrDocument[] from(InputStreamResource solrDocumentAsResource) throws IOException {

    JsonFactory jsonFactory = new JsonFactory();

    try (
        InputStream inputStream = solrDocumentAsResource.getInputStream();
        JsonParser parser = jsonFactory.createParser(inputStream)
    ){
      //01. parse as BaseSearch list
      var OBJECT_MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();
      OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      return OBJECT_MAPPER.readValue(parser, SolrDocument[].class);
    } catch (IOException e) {
      log.error("Error parsing SolrDocument JSON array stream", e);
      throw e;
    }

  }

}
