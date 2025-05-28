package org.zim.gamsapi.Integration.CoreSearch;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.auditing.AuditingHandler;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestProject;

/**
 * Integration test for the ElasticSearch service.
 * This class extends the base integration test class to provide
 * a specific context for testing ElasticSearch functionalities.
 */
@Slf4j
public class CoreSearchServiceIT extends CoreSearchIntegrationTest {

  // disables auditing
  @MockBean
  private AuditingHandler auditingHandler;

  @Autowired
  private CoreSearchService coreSearchService;

  @Autowired
  private CoreSearchRepository coreSearchRepository;

  @Autowired
  private IProjectRepository projectRepository;

  @Autowired
  private IDigitalObjectRepository digitalObjectRepository;

  @Nested
  public class IndexObject {

    /**
     * Tests if the ElasticSearch service can save a DigitalObject
     * and verifies that the object is stored as expected.
     */
    @Test
    public void storesExpectedObject(){

      final Project TEST_PROJECT = TestProject.generate();
      projectRepository.save(
          TEST_PROJECT
      );

      final DigitalObject TEST_DIGITAL_OBJECT = TestDigitalObject.generate();
      digitalObjectRepository.save(
          TEST_DIGITAL_OBJECT
      );

      coreSearchService.indexObject(
          TEST_PROJECT.getProjectAbbr(),
          TEST_DIGITAL_OBJECT.getId()
      );

      Assertions.assertThat(
          coreSearchRepository.existsById(TEST_DIGITAL_OBJECT.getId())
      ).isTrue();

    }

  }

}
