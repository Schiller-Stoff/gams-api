package org.zim.gamsapi.DigitalObject;

import lombok.extern.slf4j.Slf4j;
import org.zim.gamsapi.Project.Project;

import java.util.Set;

@Slf4j
public class DigitalObjectBuilder {

  private final DigitalObject digitalObject = new DigitalObject();

  public DigitalObjectBuilder id(String id) {
    digitalObject.setId(id);
    return this;
  }

  public DigitalObjectBuilder parent(DigitalObject parent) {
    digitalObject.setParent(parent);
    return this;
  }

  public DigitalObjectBuilder objectType(String objectType) {
    digitalObject.setObjectType(objectType);
    return this;
  }

  public DigitalObjectBuilder published(java.util.Date published) {
    digitalObject.setPublished(published);
    return this;
  }

  public DigitalObjectBuilder project(Project project) {
    digitalObject.setProject(project);
    return this;
  }

  public DigitalObjectBuilder baseMetadata(org.zim.gamsapi.MetadataBaseEntity baseMetadata) {
    digitalObject.setBaseMetadata(baseMetadata);
    return this;
  }

  public DigitalObjectBuilder types(Set<String> types) {
    digitalObject.setTypes(types);
    return this;
  }

  public DigitalObject build() {
    if((digitalObject.getId() == null) || digitalObject.getId().isEmpty()){
      String msg = String.format("Digital object ID must not be null or empty! Happened at class %s", this.getClass().getName());
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    // TODO add more validation

    return digitalObject;
  }


}
