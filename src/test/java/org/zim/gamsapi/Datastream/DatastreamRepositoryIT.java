package org.zim.gamsapi.Datastream;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestDatastream;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestProject;
import java.util.Set;

/**
 * Integration test for the DatastreamRepository.
 */
@Slf4j
public class DatastreamRepositoryIT extends IntegrationTest {

    @Autowired
    IDatastreamRepository datastreamRepository;

    @Autowired
    IDigitalObjectRepository digitalObjectRepository;

    @Autowired
    IProjectRepository projectRepository;

    private Project testProject;
    private DigitalObject testDigitalObject;
    private Datastream testDatastream;


    /**
     * Creates a test project, digital object and datastream before each test.
     * All created objects are deleted after each test.
     * All are in child relationship between project -> digital object -> datastream
     * no additional data is provided.
     */
    @BeforeEach
    public void setup(){
        // first create and save test project
        testProject = Project.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build();
        projectRepository.save(testProject);

        testDigitalObject = DigitalObject.builder().id(TestDigitalObject.DIGITAL_OBJECT_ID.getValue()).project(testProject).build();
        testDigitalObject = digitalObjectRepository.save(testDigitalObject);
        // ! establish reverse bidirectional relationship ! otherwise the cascade delete will not work
        testProject.setDigitalObjects(Set.of(testDigitalObject));

        testDatastream = Datastream.builder().digitalObject(testDigitalObject).dsid(TestDatastream.DSID.getValue()).build();
        datastreamRepository.save(testDatastream);
        // ! establish reverse bidirectional relationship ! otherwise the cascade delete will not work
        testDigitalObject.setDatastreams(Set.of(testDatastream));
    }

    /**
     * Deletes the test data after each test.
     */
    @AfterEach
    public void tearDown(){
        // verify that the test data is being deleted after each test
        projectRepository.delete(testProject);
        Assertions.assertThat(
                projectRepository.findById(testProject.getProjectAbbr()))
                .isNotNull()
                .isNotPresent();

        // assert deletion of parent object
        Assertions.assertThat(
                        digitalObjectRepository.findById(testDigitalObject.getId()))
                .isNotNull()
                .isNotPresent();

        // verify that the datastream was deleted
        Assertions.assertThat(
                        datastreamRepository.findById(testDatastream.getGlobalId()))
                .isNotNull()
                .isNotPresent();
    }


    /**
     * Tests if a saved datastream exists with the expected globalID.
     */
    @Test
    public void saveDatastreamExistsWithExpectedID() {
        Datastream datastream = datastreamRepository.save(Datastream.builder().build());
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
        Datastream datastream = datastreamRepository.save(Datastream.builder().build());
        Assertions.assertThat(
                        datastreamRepository.findById(datastream.getGlobalId()))
                .isNotNull()
                .isPresent();
        log.info("****** Deleting datastream: {}", datastream);
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
    public class CascadingDelete {

        /**
         * Verifies that a datastream is deleted if its parent digital object was deleted.
         */
        @Test
        public void datastreamCascadeDeletedFromDigitalObject(){

            // delete parent object
            digitalObjectRepository.delete(testDigitalObject);

            // verify that the object was deleted
            Assertions.assertThat(
                            digitalObjectRepository.findById(testDigitalObject.getId()))
                    .isNotNull()
                    .isNotPresent();

            // verify that the datastream was deleted
            Assertions.assertThat(
                            datastreamRepository.findById(testDatastream.getGlobalId()))
                    .isNotNull()
                    .isNotPresent();

        }


        @Test
        public void datastreamCascadeDeletedFromProject(){

            // delete parent object
            projectRepository.delete(testProject);

            // verify that the object was deleted
            Assertions.assertThat(
                            projectRepository.findById(testProject.getProjectAbbr()))
                    .isNotNull()
                    .isNotPresent();

            Assertions.assertThat(
                            digitalObjectRepository.findById(testDigitalObject.getId()))
                    .isNotNull()
                    .isNotPresent();

            // verify that the datastream was deleted
            Assertions.assertThat(
                            datastreamRepository.findById(testDatastream.getGlobalId()))
                    .isNotNull()
                    .isNotPresent();

        }

        @Test
        public void deletionOfDatastreamDoesNotDeleteParentDigitalObject(){

            // delete parent object
            datastreamRepository.delete(testDatastream);

            // datastream deleted
            Assertions.assertThat(
                        datastreamRepository.findById(testDatastream.getGlobalId()))
                    .isNotNull()
                    .isNotPresent();

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
        public void datastreamIsFindabelViaDsid(){
            datastreamRepository.findByDigitalObjectAndDsid(testDigitalObject, TestDatastream.DSID.getValue())
                    .ifPresentOrElse(
                            datastream1 -> Assertions.assertThat(datastream1)
                                    .isNotNull()
                                    .isEqualTo(testDatastream),
                            () -> Assertions.fail("Datastream not found")
                    );

        }


    }



}
