package org.zim.gamsapi.Datastream;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.MetadataBaseEntity;

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

  public DatastreamBuilder data(byte[] data) {
    datastream.setData(data);
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

  public Datastream build() {
    if(datastream.getDigitalObject() == null) {
      String msg = String.format("Encountered null digital object at %s Digital object must be set during builder process.", this.getClass().getName());
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    if((datastream.getDigitalObject().getId()) == null || datastream.getDigitalObject().getId().isEmpty()){
      String msg = String.format("Encountered null or empty digital object id at %s. Digital object identifier must be set during builder process.", this.getClass().getName());
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    if((datastream.getDsid() == null) || datastream.getDsid().isEmpty()) {
      String msg = String.format("Encountered null or empty dsid at %s. Datastream identifier must be set during builder process.", this.getClass().getName());
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    return datastream;
  }

}
