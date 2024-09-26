package org.zim.gamsapi.Datastream;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamDetailsView;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestDatastream;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestMetadataBaseEntity;
import org.zim.gamsapi.enums.TestProject;

/**
 * Integration test for the DatastreamRepository.
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatastreamRepositoryIT extends IntegrationTest {

    /**
     * Mocks the auditing behavior of the app.
     * Without mocking the auditing handler, the tests would fail because of the missing oauth2 user info
     */
    @MockBean
    private AuditingHandler auditingHandler;

    @Autowired
    IDatastreamRepository datastreamRepository;

    @Autowired
    IDigitalObjectRepository digitalObjectRepository;

    @Autowired
    IProjectRepository projectRepository;

    Project testProject;
    DigitalObject testDigitalObject;
    Datastream testDatastream;

    MetadataBaseEntity testMetadataBaseEntity = TestMetadataBaseEntity.generate();


    /**
     * Creates a test project, digital object and datastream before each test.
     * All created objects are deleted after each test.
     * All are in child relationship between project -> digital object -> datastream
     * no additional data is provided.
     */
    @BeforeEach
    public void setup(){

        testDigitalObject = TestDigitalObject.generate();
        testProject = testDigitalObject.getProject();
        testDatastream = TestDatastream.generate(testDigitalObject);

        projectRepository.save(testProject);
        digitalObjectRepository.save(testDigitalObject);
        datastreamRepository.save(testDatastream);

    }

    /**
     * Tests if a saved datastream exists with the expected globalID.
     */
    @Test
    public void saveDatastreamExistsWithExpectedID() {
        // using the DatastreamId from the test datastream
        Datastream datastream = TestDatastream.generate(testDigitalObject, "SOME_RANDOM_DSID");
        datastreamRepository.save(datastream);

        Assertions.assertThat(
                datastreamRepository.findById(datastream.deriveDatastreamId()))
                .isNotNull()
                .isPresent()
                .get()
                .extracting(Datastream::deriveDatastreamId)
                .isEqualTo(datastream.deriveDatastreamId()
        );
        // clean up and check if successfully deleted
        datastreamRepository.delete(datastream);
        Assertions.assertThat(
                datastreamRepository.findById(datastream.deriveDatastreamId()))
                .isNotNull()
                .isNotPresent();

    }

    /**
     * Tests if the datastream with the expected globalID was deleted.
     */
    @Test
    public void deleteDatastreamRemovesDatastream() {
        Datastream datastream = datastreamRepository.save(
            TestDatastream.generate(testDigitalObject, "SOME_RANDOM_DSID_45123")
        );
        Assertions.assertThat(
                        datastreamRepository.findById(datastream.deriveDatastreamId()))
                .isNotNull()
                .isPresent();

        datastreamRepository.delete(datastream);
        Assertions.assertThat(
                datastreamRepository.findById(datastream.deriveDatastreamId()))
                .isNotNull()
                .isNotPresent();

    }


    /**
     * Tests if a datastream that does not exist returns an empty optional.
     */
    @Test
    public void findByIdReturnsEmptyOptionalIfDatastreamDoesNotExist() {

        DatastreamId datastreamId = DatastreamId.builder()
            .dsid("NOT_THERE")
            .digitalObject("NOT_THERE")
            .build();

        Assertions.assertThat(
                datastreamRepository.findById(datastreamId))
                .isNotNull()
                .isNotPresent();

    }


    /**
     * Tests against delete cascading.
     */
    @Nested
    public class TestCascadingDelete {

        @Test
        public void digitalObjectWithSavedDatastreamsCannotBeDeleted(){

            DigitalObject toBeDeleted = TestDigitalObject.generate();

            digitalObjectRepository.save(toBeDeleted);
            Datastream savedDatastream = datastreamRepository.save(
                TestDatastream.generate(toBeDeleted)
            );

            // try to delete the object if datastream is still available
            org.junit.jupiter.api.Assertions.assertThrows(
                DataIntegrityViolationException.class,
                () -> digitalObjectRepository.delete(toBeDeleted)
            );


            // verify deletion is not cascaded
            Assertions.assertThat(
                datastreamRepository.findById(savedDatastream.deriveDatastreamId())
            ).isPresent();


        }


        @Test
        public void datastreamDoesNotCascadeDeletedFromProject(){
            // because the testdatastream should still exist
            org.junit.jupiter.api.Assertions.assertThrows(
                DataIntegrityViolationException.class,
                () -> projectRepository.delete(testProject)
            );
        }

        @Test
        public void deletionOfDatastreamDoesNotDeleteParentDigitalObject(){

            DigitalObject digitalObject = TestDigitalObject.generate(TestProject.PROJECT_ABBR.getValue(), TestProject.PROJECT_ABBR.getValue() + ".an.object");

            digitalObjectRepository.save(digitalObject);

            Datastream datastreamToBeDeleted = TestDatastream.generate(digitalObject, "DSID_TO_BE_DELETED");

            datastreamToBeDeleted = datastreamRepository.save(datastreamToBeDeleted);

            // saved datastream should exist
            Assertions.assertThat(
                datastreamRepository.findById(datastreamToBeDeleted.deriveDatastreamId()))
                    .isNotNull()
                    .isPresent();


            // delete datastream
            datastreamRepository.delete(datastreamToBeDeleted);
            digitalObjectRepository.delete(digitalObject);

            // datastream deleted
            Assertions.assertThat(
                        datastreamRepository.findById(datastreamToBeDeleted.deriveDatastreamId()))
                    .isNotNull()
                    .isNotPresent();

            // object + project still available!
            Assertions.assertThat(
                        digitalObjectRepository.findById(testDigitalObject.getId()))
                    .isNotNull()
                    .isPresent();

            // additionally check if project is still available
            Assertions.assertThat(projectRepository.findById(testProject.getProjectAbbr()))
                    .isNotNull()
                    .isPresent();


        }

    }


    @Nested
    public class TestCustomRepositoryMethods {

        @Test
        @Transactional
        public void findsExpectedDatastreamViaDsid(){

            DatastreamId datastreamId = DatastreamId.builder()
                .dsid(TestDatastream.DSID.getValue())
                .digitalObject(testDigitalObject.getId())
                .build();

            var foundDatastream = datastreamRepository.findById(datastreamId).orElseThrow();

            String foundDsid = foundDatastream.getDsid();
            String foundDigitalObjectId = foundDatastream.getDigitalObject().getId();

            Assertions.assertThat(foundDsid)
                    .isNotEmpty()
                    .isEqualTo(foundDatastream.getDsid());

            Assertions.assertThat(foundDigitalObjectId)
                .isNotEmpty()
                .isEqualTo(foundDatastream.getDigitalObject().getId());



        }

        @Test
        @Transactional
        public void deleteByDigitalObjectAndDsidDeletesDatastream(){

            Datastream datastreamToBeDeleted = TestDatastream.generate(testDigitalObject);
            datastreamRepository.save(datastreamToBeDeleted);

            Assertions.assertThat(
                datastreamRepository.findById(datastreamToBeDeleted.deriveDatastreamId())
            ).isNotNull().isPresent();

            datastreamRepository.delete(datastreamToBeDeleted);

            Assertions.assertThat(
                datastreamRepository.findById(datastreamToBeDeleted.deriveDatastreamId())
            ).isNotNull().isNotPresent();


        }

        @Test
        public void deleteAllRemovesTestDatastream(){

            // first test datastream is available
            Assertions.assertThat(
                    datastreamRepository.findById(testDatastream.deriveDatastreamId()))
                    .isNotNull()
                    .isPresent();

            datastreamRepository.deleteAll();

            // test datastream is not available anymore
            Assertions.assertThat(
                    datastreamRepository.findById(testDatastream.deriveDatastreamId()))
                    .isNotNull()
                    .isNotPresent();

        }

        @Test
        public void findAllByDigitalObjectIdReturnsDatastreamDetailsViewWithSameDsid(){
            datastreamRepository.findAllByDigitalObjectId(testDigitalObject.getId())
                    .forEach(datastreamDetailsView -> {
                        Assertions.assertThat(datastreamDetailsView)
                                .isNotNull()
                                .extracting(IDatastreamDetailsView::getDsid)
                                .isEqualTo(testDatastream.getDsid());
                    });
        }

        @Test
        public void findDatastreamDetailsViewByDigitalObjectAndDsidReturnsDatastreamDetailsView(){

            DatastreamId datastreamId = DatastreamId.builder()
                .dsid(TestDatastream.DSID.getValue())
                .digitalObject(testDigitalObject.getId())
                .build();

            datastreamRepository.findDatastreamDetailsViewByDigitalObject_IdAndDsid(datastreamId.getDigitalObject(), datastreamId.getDsid())
                    .ifPresentOrElse(
                            datastreamDetailsView -> Assertions.assertThat(datastreamDetailsView)
                                    .isNotNull()
                                    .extracting(IDatastreamDetailsView::getDsid)
                                    .isEqualTo(testDatastream.getDsid()),
                            () -> Assertions.fail("Datastream not found")
                    );
        }


    }


    @Nested
    public class Constraints {

        @Nested
        public class IDGeneration {
            @Test
            public void throwsIfDsidIsNull(){

                Datastream datastream = new DatastreamBuilder()
                    .dsid("RANDOM_DSID_123456")
                    .digitalObject(testDigitalObject)
                    .baseMetadata(testMetadataBaseEntity)
                    .build();

                datastream.setDsid(null);

                org.junit.jupiter.api.Assertions.assertThrows(
                    // exception is being thrown by ConstraintViolationException beneath
                    TransactionSystemException.class,
                    () -> datastreamRepository.save(datastream)
                );
            }

            @Test
            public void throwsIfDigitalObjectIsNull(){

                Datastream datastream = new DatastreamBuilder()
                    .dsid("RANDOM_DSID_123456")
                    .digitalObject(testDigitalObject)
                    .baseMetadata(testMetadataBaseEntity)
                    .build();

                datastream.setDigitalObject(null);

                org.junit.jupiter.api.Assertions.assertThrows(
                    // exception is being thrown because composite primary key fails to be set
                    JpaSystemException.class,
                    () -> datastreamRepository.save(datastream)
                );
            }

        }

        @Nested
        public class MetadataBaseEntityValidation {

            @Test
            public void throwsIfBaseMetadataIsNull(){

                Datastream datastream = new DatastreamBuilder()
                    .dsid("RANDOM_DSID_123456")
                    .digitalObject(testDigitalObject)
                    .baseMetadata(testMetadataBaseEntity)
                    .build();

                datastream.setBaseMetadata(null);

                org.junit.jupiter.api.Assertions.assertThrows(
                    TransactionSystemException.class,
                    () -> datastreamRepository.save(datastream)
                );
            }

            @Test
            public void throwsIfMetadataDescriptionIsTooShort(){

                MetadataBaseEntity metadataBaseEntity = TestMetadataBaseEntity.generate();
                metadataBaseEntity.setDescription("1");

                Datastream datastream = new DatastreamBuilder()
                    .dsid("RANDOM_DSID_123456")
                    .digitalObject(testDigitalObject)
                    .baseMetadata(metadataBaseEntity)
                    .build();

                org.junit.jupiter.api.Assertions.assertThrows(
                    TransactionSystemException.class,
                    () -> datastreamRepository.save(datastream)
                );
            }

            @Test
            public void throwsIfMetadataRightsIsNull(){

                  MetadataBaseEntity metadataBaseEntity = TestMetadataBaseEntity.generate();
                  metadataBaseEntity.setRights(null);

                  Datastream datastream = new DatastreamBuilder()
                      .dsid("RANDOM_DSID_123456")
                      .digitalObject(testDigitalObject)
                      .baseMetadata(metadataBaseEntity)
                      .build();

                  org.junit.jupiter.api.Assertions.assertThrows(
                      TransactionSystemException.class,
                      () -> datastreamRepository.save(datastream)
                  );
            }

        }
    }

    @Test
    public void throwsIfObjectIsNotSaved(){

        DigitalObject unsavedObject = TestDigitalObject.generate();
        unsavedObject.setId("NOT_SAVED_OBJECT_923");

        Datastream aDatastream = TestDatastream.generate(unsavedObject, "RANDOM_DSID_123456");


        org.junit.jupiter.api.Assertions.assertThrows(
            DataIntegrityViolationException.class,
            () -> datastreamRepository.save(aDatastream)
        );

    }



}
