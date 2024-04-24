package org.zim.gamsapi.DigitalObject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.MetadataBaseEntityBuilder;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
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

    MetadataBaseEntity testMetadataBaseEntity = new MetadataBaseEntityBuilder()
        .title("test-title")
        .rights("test-rights")
        .publisher("test-publisher")
        .creator("test-creator")
        .description("test-description")
        .build();

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

            final String RANDOM_PID = "RANDOM_PID";

            digitalObjectRepository.save(
                new DigitalObjectBuilder()
                    .id(RANDOM_PID)
                    .project(testProject)
                    .baseMetadata(testMetadataBaseEntity)
                    .build()
            );

            Assertions.assertThat(digitalObjectRepository.findById(RANDOM_PID))
                .isNotNull()
                .isPresent();

            // cleanup
            digitalObjectRepository.deleteById(RANDOM_PID);


        }


    }

    @Test
    public void testFindByPid() {

        final String RANDOM_PID = "RANDOM_PID";

        digitalObjectRepository.save(
            new DigitalObjectBuilder()
                .id(RANDOM_PID)
                .project(testProject)
                .baseMetadata(testMetadataBaseEntity)
                .build()
        );

        digitalObjectRepository.findById(RANDOM_PID);

        Assertions.assertThat(digitalObjectRepository.findById(RANDOM_PID))
            .isNotNull()
            .isPresent();

        // cleanup
        digitalObjectRepository.deleteById(RANDOM_PID);

    }

    @Test
    public void testFindByProjectAbbr() {

        final String RANDOM_PID = "RANDOM_PID";

        digitalObjectRepository.save(
            new DigitalObjectBuilder()
                .id(RANDOM_PID)
                .project(testProject)
                .baseMetadata(testMetadataBaseEntity)
                .build()
        );

        List<DigitalObject> digitalObjects = digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbr(testProject.getProjectAbbr());

        Assertions.assertThat(digitalObjects)
            .isNotNull()
            .isExactlyInstanceOf(ArrayList.class)
            .first()
            .extracting(DigitalObject::getId)
            .isEqualTo(RANDOM_PID);

        // cleanup
        digitalObjectRepository.deleteById(RANDOM_PID);
    }

    @Test
    @Transactional
    public void testDeleteAllByProjectAbbr() {

        final String RANDOM_PID = "RANDOM_PID";

        digitalObjectRepository.save(
            new DigitalObjectBuilder()
                .id(RANDOM_PID)
                .project(testProject)
                .baseMetadata(testMetadataBaseEntity)
                .build()
        );

        digitalObjectRepository.deleteAllByProject_ProjectAbbr(testProject.getProjectAbbr());

        List<DigitalObject> digitalObjects = digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbr(testProject.getProjectAbbr());

        Assertions.assertThat(digitalObjects)
            .isNotNull()
            .isEmpty();

    }

    @Test
    public void testDeleteAll() {

        final String RANDOM_PID = "RANDOM_PID";

        digitalObjectRepository.save(
            new DigitalObjectBuilder()
                .id(RANDOM_PID)
                .project(testProject)
                .baseMetadata(testMetadataBaseEntity)
                .build()
        );

        digitalObjectRepository.deleteAll();

        Assertions.assertThat(
            digitalObjectRepository.findById(RANDOM_PID)
        ).isNotNull().isEmpty();
    }





}