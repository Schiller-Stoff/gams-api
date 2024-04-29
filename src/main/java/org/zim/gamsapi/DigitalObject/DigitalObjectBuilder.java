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

  public DigitalObjectBuilder parent(String parentId) {
    // TODO test this!
    DigitalObject parent = new DigitalObject();
    parent.setId(parentId);
    // parent project is being set in build method
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

  //TODO remove this? - there might be contradicting information in the project object
  public DigitalObjectBuilder project(Project project) {
    digitalObject.setProject(project);
    return this;
  }

  public DigitalObjectBuilder project(String project) {
    digitalObject.setProject(Project.builder().projectAbbr(project).build());
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

    if((digitalObject.getProject() == null) || digitalObject.getProject().getProjectAbbr().isEmpty()){
      String msg = String.format("DigitalObject's project must not be null or it's abbreviation empty! Happened at class %s and object %s", this.getClass().getName(), this);
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    // parent object has in any case same project as the current object
    if(digitalObject.getParent() != null){
      digitalObject.getParent().setProject(digitalObject.getProject());
    }

    return digitalObject;
  }


}
