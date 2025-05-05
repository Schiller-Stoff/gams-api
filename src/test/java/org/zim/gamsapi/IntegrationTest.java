package org.zim.gamsapi;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.utility.DockerImageName;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.GAMSCollection.IGAMSCollectionRepository;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;

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
  @MockBean
  ClientRegistrationRepository clientRegistrationRepository;

  @Autowired
  IProjectRepository projectRepository;

  @Autowired
  IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  IDatastreamRepository datastreamRepository;

  @Autowired
  IDatastreamContentRepository datastreamContentRepository;;

  @Autowired
  IDublinCoreEntryRepository dublinCoreElementRepository;

  @Autowired
  IGAMSCollectionRepository collectionRepository;

  @Autowired
  EventCaptureListener eventCaptureListener;

  // First launch postgres for all integration tests
  static final PostgreSQLContainer<?> postgres;

  static final SolrContainer solr;

  // setup of test-containers: https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/
  static {
    postgres = new PostgreSQLContainer<>("postgres:13-alpine");
    postgres.start();

    // setup of solr (without same config as in docker-compose)
    solr = new SolrContainer(DockerImageName.parse("solr:9.2.1"));
    solr.withCommand("solr-precreate", "gams");
    solr.start();
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);

    // set solr host and port
    registry.add("gams.docker.baseSearchUrl", () -> String.format("""
        http://%s:%s""", solr.getHost(), solr.getSolrPort()));
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

  /**
   * After each test, performs a system wipe so that the next test can start with a clean slate.
   */
  @AfterEach
  public void tearDown(){
    eventCaptureListener.clearEvents();
    datastreamContentRepository.deleteAll();
    dublinCoreElementRepository.deleteAll();
    datastreamRepository.deleteAll();
    collectionRepository.deleteAll();
    digitalObjectRepository.deleteAll();
    projectRepository.deleteAll();
  }


}
