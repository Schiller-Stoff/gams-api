package org.ddh.gamsapi.domain.Project;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;
import org.ddh.gamsapi.domain.Project.dto.ProjectDetailsDTO;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectNotFoundException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProjectServiceIT extends IntegrationTest {

  // Deactivates the auditing process.
  @MockitoBean
  private AuditingHandler auditingHandler;

  @Autowired
  private TestDataBuilder testDataBuilder;

  private TestDataSet testDataSet;

  @Autowired
  IProjectRepository projectRepository;
  @Autowired
  private ProjectService projectService;

  @BeforeEach
  void setup(){
    testDataSet = testDataBuilder.buildTestDataSet();
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


}
