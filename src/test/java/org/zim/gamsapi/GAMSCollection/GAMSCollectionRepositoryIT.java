package org.zim.gamsapi.GAMSCollection;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.auditing.AuditingHandler;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestGAMSCollection;


/**
 * Integration test for the GAMSCollectionRepository.
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GAMSCollectionRepositoryIT extends IntegrationTest {

  // deactivates auditing
  @MockBean
  private AuditingHandler auditingHandler;

  @Autowired
  private IGAMSCollectionRepository collectionRepository;

  @Autowired
  private IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  private IProjectRepository projectRepository;

  private Project testProject;

  private DigitalObject testDigitalObject;

  @BeforeEach
  public void setUp() {
    testDigitalObject = TestDigitalObject.generate();
    testProject = testDigitalObject.getProject();
    projectRepository.save(testProject);
    digitalObjectRepository.save(testDigitalObject);

  }

  @Nested
  public class CASCADING {

    @Test
    public void deletionOfADigitalObjectStillReferencedByACollectionThrows(){

      final GAMSCollection TEST_GAMS_COLLECTION = TestGAMSCollection
          .generate();
      collectionRepository.save(TEST_GAMS_COLLECTION);
      Assertions.assertThatThrownBy(() -> {
        digitalObjectRepository.delete(testDigitalObject);
      }).isInstanceOf(DataIntegrityViolationException.class);

    }


  }


}
