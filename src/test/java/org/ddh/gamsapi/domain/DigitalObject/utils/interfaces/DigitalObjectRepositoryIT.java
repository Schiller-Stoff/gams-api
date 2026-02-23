package org.ddh.gamsapi.domain.DigitalObject.utils.interfaces;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.Project.Project;
import org.ddh.gamsapi.domain.Project.ProjectBuilder;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DigitalObjectRepositoryIT extends IntegrationTest {

    @Autowired
    IDigitalObjectRepository digitalObjectRepository;

    @Autowired
    IProjectRepository projectRepository;

    @Autowired
    private TestDataBuilder testDataBuilder;

    Project testProject;

    @MockitoBean
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

    @Nested
    public class DeleteDigitalObjects {

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

    }

    @Nested
    public class FindingObjects {

        private TestDataSet testDataSet;

        @BeforeEach
        public void setup(){
            testDataSet = testDataBuilder.buildTestDataSet();
        }

        @Test
        public void testFindByPid() {
            Assertions.assertThat(digitalObjectRepository.findById(testDataSet.digitalObject().getId()))
                .isNotNull()
                .isPresent();
        }

        @Test
        public void testFindByProjectAbbr() {

            List<DigitalObject> digitalObjects = digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbr(testDataSet.project().getProjectAbbr());

            Assertions.assertThat(digitalObjects)
                .isNotNull()
                .isExactlyInstanceOf(ArrayList.class)
                .first()
                .extracting(DigitalObject::getId)
                .isEqualTo(testDataSet.digitalObject().getId());

        }





        @Nested
        public class FindMaxLastModifiedDateByProjectAbbr {


            @Test
            public void returnsExpectedModifiedDate(){
                Assertions.assertThat(
                    digitalObjectRepository.findMaxLastModifiedDateByProjectAbbr(testProject.getProjectAbbr()).get()
                ).isEqualTo(testDataSet.digitalObject().getModified());
            }

            @Test
            public void returnsExpectedNewestModificationTimestamp(){
                // object that was created later on
                final DigitalObject laterDigitalObject = testDataBuilder.addRandomObject(testDataSet);

                // returns the singular last modified date over all digital objects in a project.
                Instant actualModifiedDate = digitalObjectRepository.findMaxLastModifiedDateByProjectAbbr(testProject.getProjectAbbr()).get();

                // the last modified date should be the same as the last saved digital object
                Assertions.assertThat(actualModifiedDate)
                    .isEqualTo(laterDigitalObject.getModified());

                // the last modified date should not be the same as the first saved digital object
                Assertions.assertThat(actualModifiedDate)
                    .isNotEqualTo(testDataSet.digitalObject().getModified());

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
                Assertions.assertThat(testDataSet.digitalObject().getCreatedBy()).isNull();
                Assertions.assertThat(testDataSet.digitalObject().getModifiedBy()).isNull();
            }

            @Test
            public void modificationAuditingPropertiesAreNotNull(){
                // first some null assertions
                Assertions.assertThat(testDataSet.digitalObject().getCreated()).isNotNull();
                Assertions.assertThat(testDataSet.digitalObject().getModified()).isNotNull();
            }

            @Test
            public void modificationAuditingPropertiesAreUpdated(){

                // save the last modified date
                Instant lastModified = testDataSet.digitalObject().getModified();

                // update the object
                testDataSet.digitalObject().setPublisher("new publisher");
                var updatedObject = digitalObjectRepository.save(testDataSet.digitalObject());

                // the last modified date should be updated
                Assertions.assertThat(updatedObject.getModified())
                    .isNotEqualTo(lastModified);

                // the last modified date should be after the last modified date
                Assertions.assertThat(updatedObject.getModified())
                    .isAfter(lastModified);

                // the creation date should be before the last modified date
                Assertions.assertThat(updatedObject.getCreated())
                    .isBefore(updatedObject.getModified());

            }


        }


    }



}