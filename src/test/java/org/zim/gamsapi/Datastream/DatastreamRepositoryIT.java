package org.zim.gamsapi.Datastream;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
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

    @AfterEach
    public void tearDown(){
        // verify that the test data is being deleted after each test
        projectRepository.delete(Project.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build());
        Assertions.assertThat(
                projectRepository.findById(TestProject.PROJECT_ABBR.getValue()))
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
        // first create and save test project
        Project project = Project.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build();
        projectRepository.save(project);
        // then create and save testobject
        DigitalObject objectToBeFound = DigitalObject.builder().id("1").project(project).build();
        digitalObjectRepository.save(objectToBeFound);

        Assertions.assertThat(
                datastreamRepository.findByDigitalObjectAndDsid(objectToBeFound, "NOT_THERE"))
                .isNotNull()
                .isNotPresent();

        // cleanup
        digitalObjectRepository.delete(objectToBeFound);
        // verify that the object was deleted
        Assertions.assertThat(
                digitalObjectRepository.findById(objectToBeFound.getId()))
                .isNotNull()
                .isNotPresent();

        projectRepository.delete(project);
        // verify that the project was deleted
        Assertions.assertThat(
                projectRepository.findById(project.getProjectAbbr()))
                .isNotNull()
                .isNotPresent();
    }

    /**
     * Verifies that a datastream is returned if it exists.
     */
    @Test
    public void findByDigitalAndDsidReturnsDatastreamIfDatastreamExists() {
        // first create and save test project
        Project project = Project.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build();
        projectRepository.save(project);

        DigitalObject digitalObject = DigitalObject.builder().id(TestDigitalObject.DIGITAL_OBJECT_ID.getValue()).project(project).build();
        digitalObject = digitalObjectRepository.save(digitalObject);

        Datastream datastream = Datastream.builder().digitalObject(digitalObject).dsid(TestDatastream.DATASTREAM_NAME.getValue()).build();
        datastreamRepository.save(datastream);
        Assertions.assertThat(
                datastreamRepository.findByDigitalObjectAndDsid(digitalObject, datastream.getDsid()))
                .isNotNull()
                .isPresent()
                .get()
                .isEqualTo(datastream);

        // cleanup
        digitalObjectRepository.delete(digitalObject);
        // verify that the object was deleted
        Assertions.assertThat(digitalObjectRepository.findById(digitalObject.getId()))
                .isNotNull()
                .isNotPresent();
        // project cleanup
        projectRepository.delete(project);
        // verify that the project was deleted
        Assertions.assertThat(
                projectRepository.findById(project.getProjectAbbr()))
                .isNotNull()
                .isNotPresent();
    }

    /**
     * Tests against delete cascading.
     */
    @Nested
    public class CascadingDeleteTest {

        /**
         * Verifies that a datastream is deleted if its parent digital object was deleted.
         */
        @Test
        public void datastreamCascadeDeletedFromDigitalObject(){
            // first create and save test project
            Project project = Project.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build();
            projectRepository.save(project);

            DigitalObject digitalObject = DigitalObject.builder().id(TestDigitalObject.DIGITAL_OBJECT_ID.getValue()).project(project).build();
            digitalObject = digitalObjectRepository.save(digitalObject);

            Datastream datastream = Datastream.builder().digitalObject(digitalObject).dsid(TestDatastream.DATASTREAM_NAME.getValue()).build();
            datastreamRepository.save(datastream);
            // ! establish reverse bidirectional relationship ! otherwise the cascade delete will not work
            digitalObject.setDatastreams(Set.of(datastream));

            // delete parent object
            digitalObjectRepository.delete(digitalObject);

            // verify that the object was deleted
            Assertions.assertThat(
                            digitalObjectRepository.findById(digitalObject.getId()))
                    .isNotNull()
                    .isNotPresent();

            // verify that the datastream was deleted
            Assertions.assertThat(
                            datastreamRepository.findById(datastream.getGlobalId()))
                    .isNotNull()
                    .isNotPresent();

        }

    }





}
