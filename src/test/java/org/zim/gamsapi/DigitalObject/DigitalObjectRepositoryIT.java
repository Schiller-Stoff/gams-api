package org.zim.gamsapi.DigitalObject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestProject;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DigitalObjectRepositoryIT extends IntegrationTest {

    private final static String PID = "testPid";


    @Autowired
    IDigitalObjectRepository repository;

    @Autowired
    IProjectRepository projectRepository;

    Project testProject;

    @BeforeAll
    public void setup(){

        testProject = Project
            .builder()
            .projectAbbr(TestProject.PROJECT_ABBR.getValue())
            .build();

        projectRepository.save(testProject);


    }


    @AfterAll
    public void tearDown(){

        repository.deleteAll();
        projectRepository.deleteAll();

        Assertions.assertThat(repository.findAll())
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

            final String RANDOM_PID = "RANDOM_PID";

            repository.save(
                new DigitalObjectBuilder()
                    .id(RANDOM_PID)
                    .project(testProject)
                    .build()
            );

            Assertions.assertThat(repository.findById(RANDOM_PID))
                .isNotNull()
                .isPresent();

            // cleanup
            repository.deleteById(RANDOM_PID);


        }


    }

    @Test
    public void testFindByPid() {

        final String RANDOM_PID = "RANDOM_PID";

        repository.save(
            new DigitalObjectBuilder()
                .id(RANDOM_PID)
                .project(testProject)
                .build()
        );

        repository.findById(RANDOM_PID);

        Assertions.assertThat(repository.findById(RANDOM_PID))
            .isNotNull()
            .isPresent();

        // cleanup
        repository.deleteById(RANDOM_PID);

    }

    @Test
    @Order(2)
    public void testFindByProjectAbbr() {

        final String RANDOM_PID = "RANDOM_PID";

        repository.save(
            new DigitalObjectBuilder()
                .id(RANDOM_PID)
                .project(testProject)
                .build()
        );

        List<DigitalObject> digitalObjects = repository.findDigitalObjectsByProject_ProjectAbbr(testProject.getProjectAbbr());

        Assertions.assertThat(digitalObjects)
            .isNotNull()
            .isExactlyInstanceOf(ArrayList.class)
            .first()
            .extracting(DigitalObject::getId)
            .isEqualTo(RANDOM_PID);

        // cleanup
        repository.deleteById(RANDOM_PID);
    }

    @Test
    @Transactional
    public void testDeleteAllByProjectAbbr() {

        final String RANDOM_PID = "RANDOM_PID";

        repository.save(
            new DigitalObjectBuilder()
                .id(RANDOM_PID)
                .project(testProject)
                .build()
        );

        repository.deleteAllByProject_ProjectAbbr(testProject.getProjectAbbr());

        List<DigitalObject> digitalObjects = repository.findDigitalObjectsByProject_ProjectAbbr(testProject.getProjectAbbr());

        Assertions.assertThat(digitalObjects)
            .isNotNull()
            .isEmpty();

    }
}