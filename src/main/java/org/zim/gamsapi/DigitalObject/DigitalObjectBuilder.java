package org.zim.gamsapi.DigitalObject;

import lombok.extern.slf4j.Slf4j;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.ProjectBuilder;

@Slf4j
public class DigitalObjectBuilder {

  private final DigitalObject digitalObject = new DigitalObject();

  public DigitalObjectBuilder id(String id) {
    digitalObject.setId(id);
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
    digitalObject.setProject(ProjectBuilder.builder().projectAbbr(project).build());
    return this;
  }

  public DigitalObjectBuilder baseMetadata(org.zim.gamsapi.MetadataBaseEntity baseMetadata) {
    digitalObject.setBaseMetadata(baseMetadata);
    return this;
  }

  public DigitalObjectBuilder publisher(String publisher) {
    digitalObject.setPublisher(publisher);
    return this;
  }

  public DigitalObjectBuilder funder(String funder) {
    digitalObject.setFunder(funder);
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

    if((digitalObject.getPublisher() == null) || digitalObject.getPublisher().isEmpty()){
      String msg = String.format("DigitalObject's publisher must not be null or empty! Happened at class %s and object %s", this.getClass().getName(), this);
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    return digitalObject;
  }


}
