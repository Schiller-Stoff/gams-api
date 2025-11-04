package org.ddh.gamsapi.domain.Project;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProjectServiceIT extends IntegrationTest {

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
  public void setup(){
    testDataSet = testDataBuilder.buildTestDataSet();
  }

  @Nested
  public class FindAllProjectAbbr {

    @Test
    public void findsExpectedProjectAbbrFromTestDataset(){
      var foundProjectAbbr = projectService.findAllProjectAbbrs();
      Assertions.assertThat(foundProjectAbbr)
          .isNotNull()
          .hasSize(1)
      ;
      Assertions.assertThat(foundProjectAbbr.get(0))
          .isEqualTo(testDataSet.project().getProjectAbbr());
    }

    @Test
    public void findsExpectedOrderedProjectAbbrs(){

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


}
