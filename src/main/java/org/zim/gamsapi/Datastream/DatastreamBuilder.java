package org.zim.gamsapi.Datastream;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.MetadataBaseEntity;

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

  public DatastreamBuilder fileName(String fileName) {
    datastream.setFileName(fileName);
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

  public DatastreamBuilder contentRestrictions(Set<String> contentRestrictions){
    datastream.setContentRestrictions(contentRestrictions);
    return this;
  }

  public DatastreamBuilder tags(Set<String> tags){
    datastream.setTags(tags);
    return this;
  }

  public Datastream build() {
    if((datastream.getDsid() == null) || datastream.getDsid().isEmpty()) {
      String msg = String.format("Encountered null or empty dsid at %s. Datastream identifier must be set during builder process.", this.getClass().getName());
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
