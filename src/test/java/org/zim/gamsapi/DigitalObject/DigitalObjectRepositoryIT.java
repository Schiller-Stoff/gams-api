package org.zim.gamsapi.DigitalObject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.MetadataBaseEntityBuilder;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestMetadataBaseEntity;
import org.zim.gamsapi.enums.TestProject;
import java.util.ArrayList;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DigitalObjectRepositoryIT extends IntegrationTest {

    private final static String PID = "testPid";


    @Autowired
    IDigitalObjectRepository digitalObjectRepository;

    @Autowired
    IProjectRepository projectRepository;

    Project testProject;

    MetadataBaseEntity testMetadataBaseEntity = TestMetadataBaseEntity.generate();

    @MockBean
    private AuditingHandler auditingHandler;

    @BeforeAll
    public void setup(){
        // TODO tests should be independent
        testProject = Project
            .builder()
            .projectAbbr(TestProject.PROJECT_ABBR.getValue())
            .build();

        projectRepository.save(testProject);


    }


    @AfterAll
    public void tearDown(){

        digitalObjectRepository.deleteAll();
        projectRepository.deleteAll();

        Assertions.assertThat(digitalObjectRepository.findAll())
            .isNotNull()
            .isEmpty();

        Assertions.assertThat(projectRepository.findAll())
            .isNotNull()
            .isEmpty();

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

            // cleanup
            digitalObjectRepository.deleteById(digitalObject.getId());


        }


    }

    @Test
    public void testFindByPid() {

        final DigitalObject digitalObject = TestDigitalObject.generate(testProject.getProjectAbbr());

        digitalObjectRepository.save(digitalObject);

        Assertions.assertThat(digitalObjectRepository.findById(digitalObject.getId()))
            .isNotNull()
            .isPresent();

        // cleanup
        digitalObjectRepository.deleteById(digitalObject.getId());

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

        // cleanup
        digitalObjectRepository.deleteById(digitalObject.getId());
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





}