package org.zim.gamsapi.Datastream;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamDetailsView;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.System.utils.DigitalObjectBuilder;
import org.zim.gamsapi.enums.TestDatastream;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestProject;

import java.util.Optional;

/**
 * Integration test for the DatastreamRepository.
 * TODO make sure that tests don't cause any side effects -> seems possible atm!
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatastreamRepositoryIT extends IntegrationTest {

    @Autowired
    IDatastreamRepository datastreamRepository;

    @Autowired
    IDigitalObjectRepository digitalObjectRepository;

    @Autowired
    IProjectRepository projectRepository;

    Project testProject;
    DigitalObject testDigitalObject;
    Datastream testDatastream;


    /**
     * Creates a test project, digital object and datastream before each test.
     * All created objects are deleted after each test.
     * All are in child relationship between project -> digital object -> datastream
     * no additional data is provided.
     */
    @BeforeAll
    public void setup(){

        testDigitalObject = new DigitalObjectBuilder(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
                .addProject(TestProject.PROJECT_ABBR.getValue())
                .add()
                .addDatastream(TestDatastream.DSID.getValue())
                .add()
                .build();

        testProject = testDigitalObject.getProject();

        testDatastream = Datastream.builder()
            .dsid(TestDatastream.DSID.getValue())
            .digitalObject(testDigitalObject)
            .build();

        projectRepository.save(testProject);
        digitalObjectRepository.save(testDigitalObject);
        datastreamRepository.save(testDatastream);

    }

    /**
     * Deletes the test data after each test.
     */
    @AfterAll
    public void tearDown(){
        //  TODO why does this throw?
        //datastreamRepository.deleteAllByDigitalObject(testDigitalObject);
        datastreamRepository.delete(testDatastream);
        digitalObjectRepository.delete(testDigitalObject);
        projectRepository.delete(testDigitalObject.getProject());

        // TODO assert that everything was deleted

    }


    /**
     * Tests if a saved datastream exists with the expected globalID.
     */
    @Test
    public void saveDatastreamExistsWithExpectedID() {
        Datastream datastream = datastreamRepository.save(
            Datastream.builder()
                .digitalObject(testDigitalObject)
                .dsid("SOME_RANDOM_DSID")
                .build()
        );

        Assertions.assertThat(
                datastreamRepository.findById(datastream.getGlobalId()))
                .isNotNull()
                .isPresent()
                .get()
                .extracting(Datastream::getGlobalId)
                .isEqualTo(datastream.getGlobalId()
        );
        // clean up and check if successfully deleted
        datastreamRepository.delete(datastream);
        Assertions.assertThat(
                datastreamRepository.findById(datastream.getGlobalId()))
                .isNotNull()
                .isNotPresent();
    }

    /**
     * Tests if the datastream with the expected globalID was deleted.
     */
    @Test
    public void deleteDatastreamRemovesDatastream() {
        Datastream datastream = datastreamRepository.save(Datastream.builder()
            .digitalObject(testDigitalObject)
            .dsid("SOME_RANDOM_DSID_45123")
            .build());
        Assertions.assertThat(
                        datastreamRepository.findById(datastream.getGlobalId()))
                .isNotNull()
                .isPresent();

        datastreamRepository.delete(datastream);
        Assertions.assertThat(
                datastreamRepository.findById(datastream.getGlobalId()))
                .isNotNull()
                .isNotPresent();
    }


    /**
     * Tests if a datastream that does not exist returns an empty optional.
     */
    @Test
    public void findByIdReturnsEmptyOptionalIfDatastreamDoesNotExist() {
        Assertions.assertThat(
                datastreamRepository.findById(5L))
                .isNotNull()
                .isNotPresent();
    }


    /**
     * Verifies that a datastream is empty optional if it does not exist.
     */
    @Test
    public void findByDigitalObjectAndDsidReturnsEmptyOptionalIfDatastreamDoesNotExist() {
        Assertions.assertThat(
                datastreamRepository.findByDigitalObjectAndDsid(testDigitalObject, "NOT_THERE"))
                .isNotNull()
                .isNotPresent();
    }

    /**
     * Verifies that a datastream is returned if it exists.
     */
    @Test
    public void findByDigitalAndDsidReturnsDatastreamIfDatastreamExists() {
        Assertions.assertThat(datastreamRepository.findByDigitalObjectAndDsid(testDigitalObject, testDatastream.getDsid()))
                .isNotNull()
                .isPresent()
                .get()
                .isEqualTo(testDatastream);
    }

    /**
     * Tests against delete cascading.
     */
    @Nested
    public class TestCascadingDelete {

        @Test
        public void digitalObjectWithSavedDatastreamsCannotBeDeleted(){

            final String TEST_DSID = "DSID_12345";
            DigitalObject toBeDeleted = new DigitalObjectBuilder("SOME_PID_12345")
                .addProject(TestProject.PROJECT_ABBR.getValue())
                    .add()
                .addDatastream(TEST_DSID)
                    .add()
                .build();

            digitalObjectRepository.save(toBeDeleted);
            Datastream savedDatastream = datastreamRepository.save(Datastream.builder()
                .digitalObject(toBeDeleted)
                .dsid(TEST_DSID)
                .build());

            // try to delete the object if datastream is still available
            org.junit.jupiter.api.Assertions.assertThrows(
                DataIntegrityViolationException.class,
                () -> digitalObjectRepository.delete(toBeDeleted)
            );


            // verify deletion is not cascaded
            Assertions.assertThat(
                datastreamRepository.findByDigitalObjectAndDsid(toBeDeleted, TEST_DSID)
            ).isPresent();

            // clean up
            datastreamRepository.delete(savedDatastream);
            digitalObjectRepository.delete(toBeDeleted);


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

            Datastream datastreamToBeDeleted = Datastream.builder()
                .dsid("DSID_TO_BE_DELETED")
                .digitalObject(testDigitalObject)
                .build();

            datastreamToBeDeleted = datastreamRepository.save(datastreamToBeDeleted);

            // saved datastream should exist
            Assertions.assertThat(
                        datastreamRepository.findByDigitalObjectAndDsid(testDigitalObject, datastreamToBeDeleted.getDsid()))
                    .isNotNull()
                    .isPresent();


            // delete datastream
            datastreamRepository.delete(datastreamToBeDeleted);

            // datastream deleted
            Assertions.assertThat(
                        datastreamRepository.findById(datastreamToBeDeleted.getGlobalId()))
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
        public void findsSameDatastreamViaDsid(){
            Assertions.assertThat(datastreamRepository.findByDigitalObjectAndDsid(testDigitalObject, TestDatastream.DSID.getValue()))
                    .isNotNull()
                    .isPresent()
                    .get()
                    .isEqualTo(testDatastream);
        }

        @Test
        public void deleteByDigitalObjectAndDsidDeletesDatastream(){
            String TEST_DSID = "DSID_FOR_DATASTREAM";
            DigitalObject digitalObject = new DigitalObjectBuilder("TO_BE_DELETED")
                    .addProject(TestProject.PROJECT_ABBR.getValue())
                    .add()
                    .addDatastream(TEST_DSID)
                    .add()
                    .build();

            digitalObject = digitalObjectRepository.save(digitalObject);

            Assertions.fail("needs refactoring!!");

//
//            Long globalDatastreamId = digitalObject.getDatastreams().iterator().next().getGlobalId();
//
//            // actual test
//            datastreamRepository.deleteByDigitalObjectAndDsid(digitalObject, TEST_DSID);
//
//            // assertions
//            Assertions.assertThat(datastreamRepository.findById(globalDatastreamId))
//                    .isNotNull()
//                    .isNotPresent();
//
//            Assertions.assertThat(datastreamRepository.findByDigitalObjectAndDsid(digitalObject, TEST_DSID))
//                    .isNotNull()
//                    .isNotPresent();
//
//            Assertions.assertThat(digitalObjectRepository.findById(digitalObject.getId()))
//                    .isNotNull()
//                    .isPresent();
//
//            // clean up
//            digitalObjectRepository.delete(digitalObject);
//            Assertions.assertThat(digitalObjectRepository.findById(digitalObject.getId()))
//                    .isNotNull()
//                    .isNotPresent();
        }

        @Test
        public void deleteAllRemovesTestDatastream(){
            // first test datastream is available
            Assertions.assertThat(
                            datastreamRepository.findById(testDatastream.getGlobalId()))
                    .isNotNull()
                    .isPresent();

            datastreamRepository.deleteAll();

            // test datastream is not available anymore
            Assertions.assertThat(
                    datastreamRepository.findById(testDatastream.getGlobalId()))
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
            datastreamRepository.findDatastreamDetailsViewByDigitalObjectAndDsid(testDigitalObject, TestDatastream.DSID.getValue())
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

        @Test
        public void saveThrowsConstraintViolationIfDigitalObjectNotAssigned(){

            Datastream datastream = Datastream.builder()
                // no digital object assigned
                .dsid("DSID")
                .build();

            org.junit.jupiter.api.Assertions.assertThrows(
                ConstraintViolationException.class,
                () -> datastreamRepository.save(datastream)
            );

        }

        @Test
        public void saveThrowsConstraintViolationIfDsidNotAssigned(){

            Datastream datastream = Datastream.builder()
                .digitalObject(testDigitalObject)
                // no dsid assigned
                .build();

            org.junit.jupiter.api.Assertions.assertThrows(
                ConstraintViolationException.class,
                () -> datastreamRepository.save(datastream)
            );

        }



    }

    @Test
    public void throwsIfObjectIsNotSaved(){

        DigitalObject unsavedObject = new DigitalObjectBuilder("UNSAVED_12345")
                .addProject(TestProject.PROJECT_ABBR.getValue())
                .add()
                .addDatastream(TestDatastream.DSID.getValue())
                .add()
                .build();

        Datastream aDatastream = Datastream.builder()
            .dsid("RANDOM_DSID_123456")
            .digitalObject(unsavedObject)
            .build();


        org.junit.jupiter.api.Assertions.assertThrows(
            InvalidDataAccessApiUsageException.class,
            () -> datastreamRepository.save(aDatastream)
        );

    }



}
