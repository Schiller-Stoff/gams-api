package org.zim.gamsapi.Project;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
  @MockitoBean
  private AuditingHandler auditingHandler;


  @Test
  public void projectDeletionFailIfDigitalObjectStillReferencesTheProject(){

    Project project = ProjectBuilder.builder()
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

    Project project = ProjectBuilder.builder()
        .projectAbbr(TestProject.PROJECT_ABBR.getValue())
        .build();

    projectRepository.save(project);

    var foundProject = projectRepository.findById(project.getProjectAbbr());
    org.assertj.core.api.Assertions.assertThat(foundProject).isPresent();
    org.assertj.core.api.Assertions.assertThat(foundProject.get())
        .isEqualTo(project);

  }

  @Nested
  public class FindLastModifiedDateByProjectAbbr {

    @Test
    public void returnsExpectedModifiedDate(){

      // contains no modification date etc.
      Project project = TestProject.generate();

      // saved project will contain modification date
      Project savedProject = projectRepository.save(project);

      var foundProjectDate = projectRepository
          .findLastModifiedDateByProjectAbbr(project.getProjectAbbr());
      org.assertj.core.api.Assertions.assertThat(foundProjectDate).isPresent();

      // method returns same time as saved project's modification date
      org.assertj.core.api.Assertions.assertThat(
          foundProjectDate.get()
      ).hasSameTimeAs(savedProject.getModified());

    }

  }

  /**
   * Tests for time based modification auditing properties of the project entity.
   * createdBy and modifiedBy are excluded.
   */
  @Nested
  public class ModificationAuditing {

    /**
     * User auditing is disabled for this test-class
     */
    @Test
    public void userAuditingFieldsShouldBeNull(){
      Project savedProject = projectRepository.save(
          TestProject.generate()
      );
      org.assertj.core.api.Assertions.assertThat(savedProject.getCreatedBy()).isNull();
      org.assertj.core.api.Assertions.assertThat(savedProject.getModifiedBy()).isNull();
    }

    @Test
    public void modificationAuditingPropertiesAreNotNull(){

      Project savedProject = projectRepository.save(
          TestProject.generate()
      );

      // first some null assertions
      org.assertj.core.api.Assertions.assertThat(savedProject.getCreated()).isNotNull();
      org.assertj.core.api.Assertions.assertThat(savedProject.getModified()).isNotNull();

    }

    @Test
    public void modificationAuditingPropertiesAreUpdated(){

      Project savedProject = projectRepository.save(
          TestProject.generate()
      );

      // save the last modified date
      java.util.Date lastModified = savedProject.getModified();

      // update the project
      savedProject.setDescription("new description");
      savedProject = projectRepository.save(savedProject);

      // check if the modification date has been updated
      org.assertj.core.api.Assertions.assertThat(
          savedProject.getModified()
      ).isAfter(lastModified);

      // modification date is different from created
      org.assertj.core.api.Assertions.assertThat(
          savedProject.getModified()
      ).isNotEqualTo(
          savedProject.getCreated()
      );

    }

  }

}
