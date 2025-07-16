package org.zim.gamsapi.GAMSCollection;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.enums.TestDataBuilder;
import org.zim.gamsapi.enums.TestDataSet;
import org.zim.gamsapi.enums.TestGAMSCollection;

/**
 * Integration test for the GAMSCollectionRepository.
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GAMSCollectionRepositoryIT extends IntegrationTest {

  // deactivates auditing
  @MockitoBean
  private AuditingHandler auditingHandler;

  @Autowired
  private IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  private TestDataBuilder  testDataBuilder;

  private TestDataSet testDataSet;
  @Autowired
  private IGAMSCollectionRepository iGAMSCollectionRepository;

  @BeforeEach
  public void setUp() {
    testDataSet = testDataBuilder.buildTestDataSet();
  }

  @Nested
  public class CASCADING {

    @Test
    @Transactional
    public void softDeletionOfADigitalObjectStillReferencedByACollectionDoesNotThrow(){
      Assertions.assertThatNoException().isThrownBy(
        () -> digitalObjectRepository.softDeleteById(testDataSet.digitalObject().getId())
      );
    }

    @Test
    public void hardDeletionOfADigitalObjectStillReferencedByACollectionThrows(){
      // saves a test collection with reference to the test object in the test dataset
      iGAMSCollectionRepository.save(TestGAMSCollection.generate());
      Assertions.assertThatThrownBy(() -> digitalObjectRepository.delete(testDataSet.digitalObject()))
          .isInstanceOf(DataIntegrityViolationException.class);
    }


  }


}
