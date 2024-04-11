package org.zim.gamsapi.Project;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.System.utils.DigitalObjectBuilder;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestProject;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProjectRepositoryIT extends IntegrationTest {


  @Autowired
  IProjectRepository projectRepository;

  @Autowired
  IDigitalObjectRepository digitalObjectRepository;


  @Test
  public void projectDeletionFailIfDigitalObjectStillReferencesTheProject(){

    Project project = Project.builder()
        .projectAbbr(TestProject.PROJECT_ABBR.getValue())
        .build();

    projectRepository.save(project);

    DigitalObject digitalObject = new DigitalObjectBuilder(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
        .addProject(project.getProjectAbbr())
        .add()
        .build();

    digitalObjectRepository.save(digitalObject);

    Assertions.assertThrows(
        DataIntegrityViolationException.class,
        () -> projectRepository.delete(project)
    );

    // cleanup
    digitalObjectRepository.delete(digitalObject);
    projectRepository.delete(project);

    ;


  }

}
