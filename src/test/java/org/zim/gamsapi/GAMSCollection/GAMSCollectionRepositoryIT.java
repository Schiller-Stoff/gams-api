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

  @BeforeEach
  public void setUp() {
    testDataSet = testDataBuilder.buildTestDataSet();
  }

  @Nested
  public class CASCADING {

    @Test
    public void softDeletionOfADigitalObjectStillReferencedByACollectionDoesNotThrow(){
      Assertions.assertThatNoException().isThrownBy(
        () -> digitalObjectRepository.delete(testDataSet.digitalObject())
      );
    }

    @Test
    @Transactional
    public void hardDeletionOfADigitalObjectStillReferencedByACollectionThrows(){
      Assertions.assertThatThrownBy(() -> digitalObjectRepository.hardDelete(testDataSet.digitalObject()))
          .isInstanceOf(DataIntegrityViolationException.class);
    }


  }


}
