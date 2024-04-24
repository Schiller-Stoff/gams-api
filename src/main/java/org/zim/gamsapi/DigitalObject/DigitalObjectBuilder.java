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

    if(digitalObject.getBaseMetadata() == null){
      String msg = String.format("DigitalObject's baseMetadata must not be null! Happened at class %s and object %s", this.getClass().getName(), this);
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    if((digitalObject.getBaseMetadata().getCreator() == null) || digitalObject.getBaseMetadata().getCreator().isEmpty()){
      String msg = String.format("DigitalObject's baseMetadata creator must not be null or empty! Happened at class %s and object %s", this.getClass().getName(), this);
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    // validate required fields in metadata base entity
    if((digitalObject.getBaseMetadata().getDescription() == null) || digitalObject.getBaseMetadata().getDescription().isEmpty()){
      String msg = String.format("DigitalObject's baseMetadata description must not be null or empty! Happened at class %s and object %s", this.getClass().getName(), this);
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    if((digitalObject.getBaseMetadata().getPublisher() == null) || digitalObject.getBaseMetadata().getPublisher().isEmpty()){
      String msg = String.format("DigitalObject's baseMetadata publisher must not be null or empty! Happened at class %s and object %s", this.getClass().getName(), this);
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    if((digitalObject.getBaseMetadata().getRights() == null) || digitalObject.getBaseMetadata().getRights().isEmpty()){
      String msg = String.format("DigitalObject's baseMetadata rights must not be null or empty! Happened at class %s and object %s", this.getClass().getName(), this);
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    if((digitalObject.getBaseMetadata().getTitle() == null) || digitalObject.getBaseMetadata().getTitle().isEmpty()){
      String msg = String.format("DigitalObject's baseMetadata title must not be null or empty! Happened at class %s and object %s", this.getClass().getName(), this);
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    return digitalObject;
  }


}
