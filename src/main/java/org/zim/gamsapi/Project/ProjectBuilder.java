package org.zim.gamsapi.Project;

import lombok.extern.slf4j.Slf4j;

/**
 * Builder class for the Project entity.
 */
@Slf4j
public class ProjectBuilder {

  private final Project project = new Project();

  public ProjectBuilder projectAbbr(String projectAbbr) {
    project.setProjectAbbr(projectAbbr);
    return this;
  }

  public ProjectBuilder title(String title) {
    project.setTitle(title);
    return this;
  }

  public ProjectBuilder description(String description) {
    project.setDescription(description);
    return this;
  }

  public Project build() {
    if((project.getProjectAbbr() == null) || project.getProjectAbbr().isEmpty()){
      String msg = String.format("Project abbreviation must not be null or empty! Happened at class %s", this.getClass().getName());
      log.error(msg);
      throw new IllegalStateException(msg);
    }
    return project;
  }

  public static ProjectBuilder builder() {
    return new ProjectBuilder();
  }

}
