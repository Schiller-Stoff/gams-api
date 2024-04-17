package org.zim.gamsapi.Datastream;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamDetailsView;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DigitalObjectBuilder;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestDatastream;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestProject;

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

        testDigitalObject = new DigitalObjectBuilder().id(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
            .project(
                Project.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build())
            .build();

        testProject = testDigitalObject.getProject();

        testDatastream = new DatastreamBuilder()
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
            new DatastreamBuilder()
                .digitalObject(testDigitalObject)
                .dsid("SOME_RANDOM_DSID")
                .build()
        );

        Assertions.assertThat(
                datastreamRepository.findById(datastream.deriveDatastreamId()))
                .isNotNull()
                .isPresent()
                .get()
                .extracting(Datastream::deriveDatastreamId)
                .isEqualTo(datastream.deriveDatastreamId()
        );
//        // clean up and check if successfully deleted
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
        Datastream datastream = datastreamRepository.save(new DatastreamBuilder()
            .digitalObject(testDigitalObject)
            .dsid("SOME_RANDOM_DSID_45123")
            .build());
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

            final String TEST_DSID = "DSID_12345";
            DigitalObject toBeDeleted = new DigitalObjectBuilder()
                .id("SOME_PID_12345")
                .project(Project.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build())
                .build();


            digitalObjectRepository.save(toBeDeleted);
            Datastream savedDatastream = datastreamRepository.save(new DatastreamBuilder()
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
                datastreamRepository.findById(savedDatastream.deriveDatastreamId())
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

            Datastream datastreamToBeDeleted = new DatastreamBuilder()
                .dsid("DSID_TO_BE_DELETED")
                .digitalObject(testDigitalObject)
                .build();

            datastreamToBeDeleted = datastreamRepository.save(datastreamToBeDeleted);

            // saved datastream should exist
            Assertions.assertThat(
                datastreamRepository.findById(datastreamToBeDeleted.deriveDatastreamId()))
                    .isNotNull()
                    .isPresent();


            // delete datastream
            datastreamRepository.delete(datastreamToBeDeleted);

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
            String TEST_DSID = "DSID_FOR_DATASTREAM";

            Datastream datastreamToBeDeleted = datastreamRepository.save(
                new DatastreamBuilder()
                    .dsid(TEST_DSID)
                    .digitalObject(testDigitalObject)
                    .build());

            Assertions.assertThat(
                datastreamRepository.findById(datastreamToBeDeleted.deriveDatastreamId())
            ).isNotNull().isPresent();

            datastreamRepository.delete(datastreamToBeDeleted);

            Assertions.assertThat(
                datastreamRepository.findById(datastreamToBeDeleted.deriveDatastreamId())
            ).isNotNull().isNotPresent();


        }

        @Test
        @Ignore
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

            // cleanup restore test datastream
            datastreamRepository.save(testDatastream);
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
    public class IdGenerationTest {

        @Test
        public void saveThrowsIfDigitalObjectIsNull(){
            Datastream datastream = new Datastream();
            datastream.setDsid("DSID");


            org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> datastreamRepository.save(datastream)
            );
        }

        @Test
        public void saveThrowsIfDsidIsNull(){

            Datastream datastream = new Datastream();
            datastream.setDigitalObject(
                new DigitalObjectBuilder().id("123456").build()
            );

            org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> datastreamRepository.save(datastream)
            );
        }

    }


    @Nested
    public class Constraints {





    }

    @Test
    public void throwsIfObjectIsNotSaved(){

        DigitalObject unsavedObject = new DigitalObjectBuilder()
            .id("UNSAVED_OBJECT")
            .project(Project.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build())
            .build();

        Datastream aDatastream = new DatastreamBuilder()
            .dsid("RANDOM_DSID_123456")
            .digitalObject(unsavedObject)
            .build();


        org.junit.jupiter.api.Assertions.assertThrows(
            DataIntegrityViolationException.class,
            () -> datastreamRepository.save(aDatastream)
        );

    }



}
