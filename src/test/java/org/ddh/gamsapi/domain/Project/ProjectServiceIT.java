package org.ddh.gamsapi.domain.Project;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.TestUtilities.TestUser;
import org.ddh.gamsapi.application.WebDeployment.WebDeployment;
import org.ddh.gamsapi.application.WebDeployment.WebDeploymentRepository;
import org.ddh.gamsapi.domain.Project.dto.ProjectDetailsDTO;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectNotEmptyException;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectNotFoundException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.ddh.gamsapi.infrastructure.System.security.IUserPrincipalAuditorMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProjectServiceIT extends IntegrationTest {

  // classes needed to deactivate auditing
  @MockitoBean
  private AuditingHandler auditingHandler;
  @MockitoBean
  private IUserPrincipalAuditorMapping userPrincipalAuditorMapping;

  @Autowired
  private TestDataBuilder testDataBuilder;

  private TestDataSet testDataSet;

  @Autowired
  IProjectRepository projectRepository;
  @Autowired
  private ProjectService projectService;
  @Autowired
  private WebDeploymentRepository webDeploymentRepository;

  @BeforeEach
  void setup(){
    testDataSet = testDataBuilder.buildTestDataSet();
    // needed because of auditing
    Mockito.when(userPrincipalAuditorMapping.getCurrentAuditor())
        .thenReturn(Optional.of(TestUser.USERNAME.getValue()));
  }

  @Nested
  class FindAllProjectAbbr {

    @Test
    void findsExpectedProjectAbbrFromTestDataset(){
      var foundProjectAbbr = projectService.findAllProjectAbbrs();
      Assertions.assertThat(foundProjectAbbr)
          .isNotNull()
          .hasSize(1)
      ;
      Assertions.assertThat(foundProjectAbbr.get(0))
          .isEqualTo(testDataSet.project().getProjectAbbr());
    }

    @Test
    void findsExpectedOrderedProjectAbbrs(){

      final Project TEST_PROJECT = ProjectBuilder.builder()
          .projectAbbr("demo")
          .build();

      projectRepository.save(
          TEST_PROJECT
      );

      var foundProjectAbbr = projectService.findAllProjectAbbrs();
      Assertions.assertThat(foundProjectAbbr)
          .isNotNull()
          .hasSize(2);

      Assertions.assertThat(foundProjectAbbr.get(0))
          .isEqualTo("demo");
      Assertions.assertThat(foundProjectAbbr.get(1))
          .isEqualTo(testDataSet.project().getProjectAbbr());


    }


  }


  @Nested
  class FindProjectDetails {

    @Test
    void returnsDetailsForExistingProject() {

      ProjectDetailsDTO result = projectService.findProjectDetails(
          testDataSet.project().getProjectAbbr()
      );

      Assertions.assertThat(result).isNotNull();
      Assertions.assertThat(result.getProjectAbbr())
          .isEqualTo(testDataSet.project().getProjectAbbr());
    }

    @Test
    void statisticsReflectTestData() {
      // testDataSet creates at least 1 digital object and 1 datastream

      ProjectDetailsDTO result = projectService.findProjectDetails(
          testDataSet.project().getProjectAbbr()
      );

      Assertions.assertThat(result.getStatistics()).isNotNull();
      Assertions.assertThat(result.getStatistics().getDigitalObjectCount())
          .isGreaterThanOrEqualTo(1);
      Assertions.assertThat(result.getStatistics().getDatastreamCount())
          .isGreaterThanOrEqualTo(1);
      Assertions.assertThat(result.getStatistics().getTotalStorageBytes())
          .isGreaterThanOrEqualTo(0);
    }

    @Test
    void emptyProjectHasZeroStatistics() {
      testDataBuilder.removeAllExceptProjects(testDataSet);

      ProjectDetailsDTO result = projectService.findProjectDetails(
          testDataSet.project().getProjectAbbr()
      );

      Assertions.assertThat(result.getStatistics().getDigitalObjectCount()).isZero();
      Assertions.assertThat(result.getStatistics().getDatastreamCount()).isZero();
      Assertions.assertThat(result.getStatistics().getTotalStorageBytes()).isZero();
    }

    @Test
    void throwsExceptionForNonExistentProject() {

      Assertions.assertThatThrownBy(
              () -> projectService.findProjectDetails("nonExistent")
          )
          .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void statisticsUpdateAfterObjectAddition() {

      ProjectDetailsDTO before = projectService.findProjectDetails(
          testDataSet.project().getProjectAbbr()
      );

      long countBefore = before.getStatistics().getDigitalObjectCount();

      // add another digital object
      testDataBuilder.addRandomObject(testDataSet);

      ProjectDetailsDTO after = projectService.findProjectDetails(
          testDataSet.project().getProjectAbbr()
      );

      Assertions.assertThat(after.getStatistics().getDigitalObjectCount())
          .isEqualTo(countBefore + 1);
    }

  }

  @Nested
  class DeleteProject {

    @Test
    @Transactional
    void successfullyDeletesEmptyProject(){

      var testProject = TestProject.generate("random123");
      Assertions.assertThat(
          projectRepository.existsById(testProject.getProjectAbbr())
      ).isFalse();

      var savedProject = projectRepository.save(TestProject.generate("random123"));
      Assertions.assertThat(
          projectRepository.existsById(testProject.getProjectAbbr())
      ).isTrue();

      projectService.deleteProject(savedProject);

      Assertions.assertThat(
          projectRepository.existsById(savedProject.getProjectAbbr())
      ).isFalse();

    }

    @Test
    @Transactional
    void deletingProjectShouldAutomaticallyUndeployWebContent() {
      // 1. Arrange: Setup Project
      var testProject = TestProject.generate("random123");
      var savedProject = projectRepository.save(testProject);

      // 2. Arrange: Setup Web Deployment
      var webDeployment = WebDeployment.builder()
          .projectAbbr(savedProject.getProjectAbbr())
          .deployedAt(Instant.now())
          .deployedBy("test-user")
          .fileCount(1)
          .totalSize(100)
          .build();

      webDeploymentRepository.save(webDeployment);

      // Verify initial state (both exist)
      Assertions.assertThat(projectRepository.existsById(savedProject.getProjectAbbr()))
          .as("Project should exist before deletion").isTrue();
      Assertions.assertThat(webDeploymentRepository.existsById(savedProject.getProjectAbbr()))
          .as("WebDeployment should exist before deletion").isTrue();

      // 3. Act: Delete the project
      org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> projectService.deleteProject(savedProject));

      // 4. Assert: Verify the event listener cascaded the deletion
      Assertions.assertThat(projectRepository.existsById(savedProject.getProjectAbbr()))
          .as("Project should be deleted from the database").isFalse();

      Assertions.assertThat(webDeploymentRepository.existsById(savedProject.getProjectAbbr()))
          .as("WebDeployment should be auto-deleted by the WebDeploymentEventListener").isFalse();
    }


  }


}
