package org.zim.gamsapi.DigitalObject;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.System.configproperties.GAMSAPIProperties;
import org.zim.gamsapi.util.container.PostgresContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IDigitalObjectRepositoryTest {

    private final static String PID = "testPid";


    @Autowired
    IDigitalObjectRepository repository;

    static PostgreSQLContainer<?> postgres = PostgresContainer.getInstance();

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
        postgres.close();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    public void saveTestObject() {
        repository.save(
                DigitalObject.builder()
                        .id(PID)
                        .project(Project.builder().projectAbbr(GAMSAPIProperties.DEMO_PROJECT_ABBR.name).build())
                        .build());
    }

    @Test
    @Order(1)
    public void testFindByPid() {

        Optional<DigitalObject> digitalObject = repository.findById(PID);

        assertThat(digitalObject)
                .isNotNull()
                .isPresent()
                .get()
                .extracting(DigitalObject::getId)
                .isEqualTo(PID);

        assertThat(digitalObject.get().getId())
                .isNotNull()
                .isEqualTo(PID);
    }

    @Test
    @Order(2)
    public void testFindByProjectAbbr() {

        List<DigitalObject> digitalObjects = repository.findDigitalObjectsByProject_ProjectAbbr(GAMSAPIProperties.DEMO_PROJECT_ABBR.name);

        assertThat(digitalObjects)
                .isNotNull()
                .isExactlyInstanceOf(ArrayList.class)
                .first()
                .extracting(DigitalObject::getId)
                .isEqualTo(PID);
    }

    @Test
    @Order(3)
    public void testDeleteAllByProjectAbbr() {

        repository.deleteAllByProject_ProjectAbbr(GAMSAPIProperties.DEMO_PROJECT_ABBR.name);

        Optional<DigitalObject> digitalObject = repository.findById(PID);

        assertThat(digitalObject).isEmpty();

    }
}