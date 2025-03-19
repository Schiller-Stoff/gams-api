package org.zim.gamsapi.DigitalObject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.ProjectBuilder;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestProject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DigitalObjectRepositoryIT extends IntegrationTest {

    private final static String PID = "testPid";


    @Autowired
    IDigitalObjectRepository digitalObjectRepository;

    @Autowired
    IProjectRepository projectRepository;

    Project testProject;

    @MockBean
    private AuditingHandler auditingHandler;

    @BeforeEach
    public void setup(){
        testProject = ProjectBuilder
            .builder()
            .projectAbbr(TestProject.PROJECT_ABBR.getValue())
            .build();

        projectRepository.save(testProject);


    }


    @Nested
    public class Save {

        @Test
        public void successfullySavesObject(){

            DigitalObject digitalObject = TestDigitalObject.generate(testProject.getProjectAbbr());

            digitalObjectRepository.save(digitalObject);

            Assertions.assertThat(digitalObjectRepository.findById(digitalObject.getId()))
                .isNotNull()
                .isPresent();


        }

        @Test
        public void savedObjectContainsExpectedProperties(){

            DigitalObject digitalObject = TestDigitalObject.generate(testProject.getProjectAbbr());

            DigitalObject savedDigitalObject = digitalObjectRepository.save(digitalObject);

            Assertions.assertThat(digitalObjectRepository.findById(digitalObject.getId()))
                .isNotNull()
                .isPresent();

            Assertions.assertThat(savedDigitalObject.getId()).isEqualTo(digitalObject.getId());
            Assertions.assertThat(savedDigitalObject.getPublisher()).isEqualTo(digitalObject.getPublisher());
            Assertions.assertThat(savedDigitalObject.getFunder()).isEqualTo(digitalObject.getFunder());
            Assertions.assertThat(savedDigitalObject.getObjectType()).isEqualTo(digitalObject.getObjectType());
            Assertions.assertThat(savedDigitalObject.getBaseMetadata()).isEqualTo(digitalObject.getBaseMetadata());
            Assertions.assertThat(savedDigitalObject.getPublished()).isEqualTo(digitalObject.getPublished());
            Assertions.assertThat(savedDigitalObject.getMainResource()).isEqualTo(digitalObject.getMainResource());

            // following fields are being defined by the database
            Assertions.assertThat(savedDigitalObject.getCreated()).isNotEqualTo(digitalObject.getCreated());
            Assertions.assertThat(savedDigitalObject.getModified()).isNotEqualTo(digitalObject.getModified());

        }

        @Test
        public void savesDigitalObjectWithMissingFunder(){
            DigitalObject digitalObject = TestDigitalObject.generate(testProject.getProjectAbbr());
            digitalObject.setFunder(null);
            DigitalObject savedDigitalObject = digitalObjectRepository.save(digitalObject);
            Assertions.assertThat(savedDigitalObject.getFunder()).isNull();
        }

        @Test
        public void savesDigitalObjectWithMissingMainResource(){
            DigitalObject digitalObject = TestDigitalObject.generate(testProject.getProjectAbbr());
            digitalObject.setMainResource(null);
            DigitalObject savedDigitalObject = digitalObjectRepository.save(digitalObject);
            Assertions.assertThat(savedDigitalObject.getMainResource()).isNull();
        }


    }

    @Test
    public void testFindByPid() {

        final DigitalObject digitalObject = TestDigitalObject.generate(testProject.getProjectAbbr());

        digitalObjectRepository.save(digitalObject);

        Assertions.assertThat(digitalObjectRepository.findById(digitalObject.getId()))
            .isNotNull()
            .isPresent();

    }

    @Test
    public void testFindByProjectAbbr() {

        final DigitalObject digitalObject = TestDigitalObject.generate(testProject.getProjectAbbr());

        digitalObjectRepository.save(digitalObject);

        List<DigitalObject> digitalObjects = digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbr(testProject.getProjectAbbr());

        Assertions.assertThat(digitalObjects)
            .isNotNull()
            .isExactlyInstanceOf(ArrayList.class)
            .first()
            .extracting(DigitalObject::getId)
            .isEqualTo(digitalObject.getId());

    }

    @Test
    @Transactional
    public void testDeleteAllByProjectAbbr() {

        final DigitalObject digitalObject = TestDigitalObject.generate(testProject.getProjectAbbr());

        digitalObjectRepository.save(digitalObject);

        digitalObjectRepository.deleteAllByProject_ProjectAbbr(testProject.getProjectAbbr());

        List<DigitalObject> digitalObjects = digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbr(testProject.getProjectAbbr());

        Assertions.assertThat(digitalObjects)
            .isNotNull()
            .isEmpty();

    }

    @Test
    public void testDeleteAll() {

        final DigitalObject digitalObject = TestDigitalObject.generate(testProject.getProjectAbbr());

        digitalObjectRepository.save(digitalObject);

        digitalObjectRepository.deleteAll();

        Assertions.assertThat(
            digitalObjectRepository.findById(digitalObject.getId())
        ).isNotNull().isEmpty();
    }



    @Nested
    public class FindMaxLastModifiedDateByProjectAbbr {


        @Test
        public void returnsExpectedModifiedDate(){

            final DigitalObject digitalObject = TestDigitalObject.generate(testProject.getProjectAbbr());

            DigitalObject savedDigitalObject = digitalObjectRepository.save(digitalObject);

            Assertions.assertThat(
                digitalObjectRepository.findMaxLastModifiedDateByProjectAbbr(testProject.getProjectAbbr()).get()
            ).hasSameTimeAs(savedDigitalObject.getModified());


        }

        @Test
        public void returnsExpectedNewestModificationTimestamp(){

            final DigitalObject digitalObject = TestDigitalObject.generate(testProject.getProjectAbbr());
            DigitalObject savedDigitalObject = digitalObjectRepository.save(digitalObject);

            // object that was created later on
            final DigitalObject laterDigitalObject = TestDigitalObject.generate(testProject.getProjectAbbr());
            DigitalObject savedLaterDigitalObject = digitalObjectRepository.save(laterDigitalObject);

            // returns the singular last modified date over all digital objects in a project.
            Date actualModfiedDate = digitalObjectRepository.findMaxLastModifiedDateByProjectAbbr(testProject.getProjectAbbr()).get();

            // the last modified date should be the same as the last saved digital object
            Assertions.assertThat(actualModfiedDate)
                .hasSameTimeAs(savedLaterDigitalObject.getModified());

            // the last modified date should not be the same as the first saved digital object
            Assertions.assertThat(actualModfiedDate)
                .isNotEqualTo(savedDigitalObject.getModified());

        }


    }


    /**
     * Tests time based modification auditing properties of the digital object entity.
     * (Without createdBy and modifiedBy)
     */
    @Nested
    public class ModificationAuditing {

        /**
         * User auditing is disabled for this test-class
         */
        @Test
        public void userAuditingFieldsShouldBeNull(){

            DigitalObject savedObject = digitalObjectRepository.save(
                TestDigitalObject.generate(testProject.getProjectAbbr())
            );

            Assertions.assertThat(savedObject.getCreatedBy()).isNull();
            Assertions.assertThat(savedObject.getModifiedBy()).isNull();

        }

        @Test
        public void modificationAuditingPropertiesAreNotNull(){

            DigitalObject savedObject = digitalObjectRepository.save(
                TestDigitalObject.generate(testProject.getProjectAbbr())
            );

            // first some null assertions
            Assertions.assertThat(savedObject.getCreated()).isNotNull();
            Assertions.assertThat(savedObject.getModified()).isNotNull();

        }

        @Test
        public void modificationAuditingPropertiesAreUpdated(){

            DigitalObject savedObject = digitalObjectRepository.save(
                TestDigitalObject.generate(testProject.getProjectAbbr())
            );

            // save the last modified date
            Date lastModified = savedObject.getModified();

            // update the object
            savedObject.setPublisher("new publisher");
            savedObject = digitalObjectRepository.save(savedObject);

            // the last modified date should be updated
            Assertions.assertThat(savedObject.getModified())
                .isNotEqualTo(lastModified);

            // the last modified date should be after the last modified date
            Assertions.assertThat(savedObject.getModified())
                .isAfter(lastModified);

            // the creation date should be before the last modified date
            Assertions.assertThat(savedObject.getCreated())
                .isBefore(savedObject.getModified());

        }


    }
}