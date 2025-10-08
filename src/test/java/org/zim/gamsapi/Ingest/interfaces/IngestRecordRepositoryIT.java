package org.zim.gamsapi.Ingest.interfaces;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.TestUtilities.*;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IngestRecordRepositoryIT extends IntegrationTest {

    /**
     * Mocks the auditing behavior of the app.
     * Without mocking the auditing handler, the tests would fail because of the missing oauth2 user info
     */
    @MockitoBean
    private AuditingHandler auditingHandler;

    @Autowired
    private IIngestRecordRepository bagEntityRepository;

    @Autowired
    private IDigitalObjectRepository digitalObjectRepository;

    @Autowired
    private TestDataBuilder testDataBuilder;

    private TestDataSet testDataSet;

    @BeforeEach
    public void setup(){
        testDataSet = testDataBuilder.buildTestDataSet();
    }

    @Test
    @Transactional
    public void savedBagEntityPointsToExpectedDigitalObject(){

        var savedObject = digitalObjectRepository.save(
                TestDigitalObject.generate(
                    testDataSet.project().getProjectAbbr(),
                 testDataSet.project().getProjectAbbr() +".12345"
                )
        );
        var bagEntity = TestIngestRecord.generate(savedObject);
        var savedBagEntity = bagEntityRepository.save(bagEntity);

        Assertions.assertThat(savedBagEntity).isNotNull();
        Assertions.assertThat(savedBagEntity.getId()).isEqualTo(savedObject.getId());

        Assertions.assertThat(savedBagEntity.getDigitalObject()).isNotNull();
        Assertions.assertThat(bagEntity.getDigitalObject()).isEqualTo(savedObject);
        Assertions.assertThat(savedBagEntity.getDigitalObject().getId()).isEqualTo(savedObject.getId());
    }

    @Test
    @Transactional
    public void findsExpectedBagEntityById() {
        var foundBagEntity = bagEntityRepository.findById(testDataSet.ingestRecord().getId());
        Assertions.assertThat(foundBagEntity).isPresent();
        Assertions.assertThat(foundBagEntity.get()).hasNoNullFieldsOrProperties();
    }

    @Test
    public void deletionOfDigitalObjectThrowsIfBagEntityExists(){
        Assertions.assertThatThrownBy(() -> {
            digitalObjectRepository.delete(testDataSet.digitalObject());
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

  /**
   * Tests if the values saved in the database are the same as in the test files in this code project.
   * (Static test classes e.g. TestDigitalObject.ID)
   */
  @Test
    public void testIngestRecordHasSameValuesAsTestBagValues(){

      var ingestRecordOptional = bagEntityRepository.findById(testDataSet.ingestRecord().getId());

      Assertions.assertThat(ingestRecordOptional).isPresent();

      var foundIngestRecord = ingestRecordOptional.get();

      Assertions.assertThat(foundIngestRecord)
          .hasNoNullFieldsOrProperties();

      Assertions.assertThat(foundIngestRecord.getId())
          .isEqualTo(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());

      Assertions.assertThat(foundIngestRecord.getBagContactMail())
          .isEqualTo(TestBag.TestBagInfo.CONTACT_EMAIL);

      Assertions.assertThat(foundIngestRecord.getBagCreatedBy())
          .isEqualTo(TestBag.TestBagSipJson.CREATED_BY);

      Assertions.assertThat(foundIngestRecord.getBagSchema())
          .isEqualTo(TestBag.TestBagSipJson.SCHEMA);

      Assertions.assertThat(foundIngestRecord.getBagSource())
          .isEqualTo(TestBag.TestBagSipJson.SOURCE);

      Assertions.assertThat(foundIngestRecord.getBaggingTimeStamp())
          .isEqualTo(TestBag.TestBagInfo.BAGGING_TIMESTAMP);

      Assertions.assertThat(foundIngestRecord.getBagPayloadOxum())
          .isEqualTo(TestBag.TestBagInfo.PAYLOAD_OXUM);


    }

}
