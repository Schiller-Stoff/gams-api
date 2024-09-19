package org.zim.gamsapi.Project;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.auditing.AuditingHandler;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestProject;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProjectRepositoryIT extends IntegrationTest {


  @Autowired
  IProjectRepository projectRepository;

  @Autowired
  IDigitalObjectRepository digitalObjectRepository;

  // disables auditing
  @MockBean
  private AuditingHandler auditingHandler;


  @Test
  public void projectDeletionFailIfDigitalObjectStillReferencesTheProject(){

    Project project = Project.builder()
        .projectAbbr(TestProject.PROJECT_ABBR.getValue())
        .build();

    projectRepository.save(project);

    DigitalObject digitalObject = TestDigitalObject.generate(project.getProjectAbbr());

    digitalObjectRepository.save(digitalObject);

    Assertions.assertThrows(
        DataIntegrityViolationException.class,
        () -> projectRepository.delete(project)
    );

  }

  @Test
  public void savedProjectIsFindable(){

    Project project = Project.builder()
        .projectAbbr(TestProject.PROJECT_ABBR.getValue())
        .build();

    projectRepository.save(project);

    org.assertj.core.api.Assertions.assertThat(
        projectRepository.findById(project.getProjectAbbr()).get()
    ).isEqualTo(project);

  }

}
