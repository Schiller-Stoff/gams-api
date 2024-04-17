package org.zim.gamsapi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

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
@Import(IntegrationTestAuditingConfiguration.class)
public abstract class IntegrationTest {

  // First launch postgres for all integration tests
  static final PostgreSQLContainer<?> postgres;

  // setup of test-containers: https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/
  static {
    postgres = new PostgreSQLContainer<>("postgres:13-alpine");
    postgres.start();
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  /**
   * Checks if required web services are reachable, like fedora6.
   */
  static {
    /*

    TODO implement add logic to check for external services?

    RestTemplate restTemplate = new RestTemplate();
    String fooResourceUrl = "http://localhost:8082";

    ResponseEntity<String> response;

    try {
      response = restTemplate.getForEntity(fooResourceUrl, String.class);
    } catch( RestClientException e){
      String msg = String.format("Fedora6 didn't reply with status 200 on integration test start! Aborting integration tests. Make sure to run required docker services via docker-compose start before running the integration tests!");
      log.error(msg);
      throw e;
    }

    HttpStatus responseStatus = response.getStatusCode();

    if(responseStatus.value() != HttpStatus.OK.value()){
      String msg = String.format("Fedora6 didn't reply with status 200 on integration test start. Got instead status code: %s - Make sure to run required docker services via docker-compose start before running the integration tests!", responseStatus);
      log.error(msg);
    }*/


  }

}
