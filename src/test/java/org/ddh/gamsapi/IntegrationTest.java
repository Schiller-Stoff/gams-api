package org.ddh.gamsapi;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.ddh.gamsapi.TestUtilities.TestCleanupService;

/**
 * Base integration-tet superclass. Must be extended by all sub integration tests
 * to avoid code duplication.
 * Checks if needed external services are running + provides necessary spring 
 * configuration like running in test profile and starting the application contexts 
 * on random ports.
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTest {

  // required for the client registration for the oauth2 process
  // otherwise the application context won't start (will try to load the oauth2 config json file)
  // https://stackoverflow.com/questions/60778556/testing-spring-security-oauth2login-enabled-applications-throws-illegalargumente
  @MockitoBean
  ClientRegistrationRepository clientRegistrationRepository;

  @Autowired
  private TestCleanupService testCleanupService;

  // First launch postgres for all integration tests
  static final PostgreSQLContainer<?> postgres;

  // setup of test-containers: https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/
  static {
    postgres = new PostgreSQLContainer<>("postgres:15.3-alpine");
    postgres.start();
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    // dynamic postgres configuration
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);

  }

  /**
   * After each test, performs a system wipe so that the next test can start with a clean slate.
   */
  @AfterEach
  public void tearDown() {
    testCleanupService.cleanup();
  }


}
