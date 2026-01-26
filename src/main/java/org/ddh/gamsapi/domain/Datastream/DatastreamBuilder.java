package org.ddh.gamsapi.domain.Datastream;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.MetadataBaseEntity;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@NoArgsConstructor
public class DatastreamBuilder {

  private final Datastream datastream = new Datastream();


  public DatastreamBuilder digitalObject(DigitalObject digitalObject) {
    datastream.setDigitalObject(digitalObject);
    return this;
  }

  public DatastreamBuilder digitalObject(String digitalObjectId) {
    DigitalObject digitalObject = new DigitalObject();
    digitalObject.setId(digitalObjectId);
    datastream.setDigitalObject(digitalObject);
    return this;
  }

  public DatastreamBuilder dsid(String dsid) {
    datastream.setDsid(dsid);
    return this;
  }

  public DatastreamBuilder mimeType(String mimeType) {
    datastream.setMimeType(mimeType);
    return this;
  }

  public DatastreamBuilder bagPath(String bagPath) {
    datastream.setBagPath(bagPath);
    return this;
  }

  public DatastreamBuilder size(Long size) {
    datastream.setSize(size);
    return this;
  }

  public DatastreamBuilder type(String type) {
    datastream.setType(type);
    return this;
  }

  public DatastreamBuilder baseMetadata(MetadataBaseEntity metadataBaseEntity){
    datastream.setBaseMetadata(metadataBaseEntity);
    return this;
  }

  public DatastreamBuilder tags(Set<String> tags){
    datastream.setTags(tags);
    return this;
  }

  public DatastreamBuilder lang(Set<String> lang){
    datastream.setLang(lang);
    return this;
  }

  public Datastream build() {
    if((datastream.getDsid() == null) || datastream.getDsid().isEmpty()) {
      String msg = "Encountered null or empty dsid at" + this.getClass().getName() + ". Datastream identifier must be set during builder process.";
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    // ensure that a hashset is created for the datastreams
    if(datastream.getTags() == null){
      datastream.setTags(new HashSet<>());
    }

    return datastream;
  }

  /**
   * Returns a new DatastreamBuilder instance.
   * @return The new DatastreamBuilder instance.
   */
  public static DatastreamBuilder builder(){
    return new DatastreamBuilder();
  }

}
