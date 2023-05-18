package org.zim.gamsapi.DigitalObject;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
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
    private final static String PROJECT_ABBR = "testProject";

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
                new DigitalObject(
                        PID, null, "TEI", PROJECT_ABBR, null
                ));
    }

    @Test
    @Order(1)
    public void testFindByPid() {

        Optional<DigitalObject> digitalObject = repository.findById(PID);

        assertThat(digitalObject)
                .isNotNull()
                .isPresent()
                .get()
                .extracting(DigitalObject::getPid, DigitalObject::getObjectType, DigitalObject::getProjectAbbr)
                .containsExactly(PID, "TEI", PROJECT_ABBR);

        assertThat(digitalObject.get().getPid())
                .isNotNull()
                .isEqualTo(PID);
    }

    @Test
    @Order(2)
    public void testFindByProjectAbbr() {

        List<DigitalObject> digitalObjects = repository.findDigitalObjectsByProjectAbbr(PROJECT_ABBR);

        assertThat(digitalObjects)
                .isNotNull()
                .isExactlyInstanceOf(ArrayList.class)
                .first()
                .extracting(DigitalObject::getPid, DigitalObject::getObjectType, DigitalObject::getProjectAbbr)
                .containsExactly(PID, "TEI", PROJECT_ABBR);
    }

    @Test
    @Order(3)
    public void testDeleteAllByProjectAbbr() {

        repository.deleteAllByProjectAbbr(PROJECT_ABBR);

        Optional<DigitalObject> digitalObject = repository.findById(PID);

        assertThat(digitalObject).isEmpty();

    }
}