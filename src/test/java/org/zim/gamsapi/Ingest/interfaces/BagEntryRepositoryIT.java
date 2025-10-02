package org.zim.gamsapi.Ingest.interfaces;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.TestUtilities.TestBagEntity;
import org.zim.gamsapi.TestUtilities.TestDataBuilder;
import org.zim.gamsapi.TestUtilities.TestDataSet;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BagEntryRepositoryIT extends IntegrationTest {

    /**
     * Mocks the auditing behavior of the app.
     * Without mocking the auditing handler, the tests would fail because of the missing oauth2 user info
     */
    @MockitoBean
    private AuditingHandler auditingHandler;

    @Autowired
    private IBagEntityRepository bagEntityRepository;

    @Autowired
    private TestDataBuilder testDataBuilder;

    private TestDataSet testDataSet;

    @BeforeEach
    public void setup(){
        if(testDataSet == null){
            testDataSet = testDataBuilder.buildTestDataSet();
        }
    }


    @Test
    @Transactional
    public void savedBagEntityPointsToExpectedDigitalObject(){

        var bagEntity = TestBagEntity.generate(testDataSet.digitalObject());

        var savedBagEntity = bagEntityRepository.save(bagEntity);

        Assertions.assertThat(savedBagEntity).isNotNull();
        Assertions.assertThat(savedBagEntity.getId()).isEqualTo(testDataSet.digitalObject().getId());

        Assertions.assertThat(savedBagEntity.getDigitalObject()).isNotNull();
        Assertions.assertThat(savedBagEntity.getDigitalObject().getId()).isEqualTo(testDataSet.digitalObject().getId());
    }


}
