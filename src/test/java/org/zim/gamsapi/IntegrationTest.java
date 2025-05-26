package org.zim.gamsapi;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
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
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.GAMSCollection.IGAMSCollectionRepository;
import org.zim.gamsapi.Integration.BaseSearch.SOLRClient;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

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
  SOLRClient solrClient;

  @Autowired
  EventCaptureListener eventCaptureListener;

  public static final String SOLR_TEST_CORE = "test";

  public static final String SOLR_GAMS_CORE = "gams";

  // First launch postgres for all integration tests
  static final PostgreSQLContainer<?> postgres;

  static final ElasticsearchContainer elasticSearch;

  //static final SolrContainer solr;

  // setup of test-containers: https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/
  static {
    postgres = new PostgreSQLContainer<>("postgres:13-alpine");
    postgres.start();

    // TODO replace with used version according spring data doc
    elasticSearch = new ElasticsearchContainer(
        "elasticsearch:9.0.1"
    );
    elasticSearch
        // disable SSL and security for testing
        .withEnv("discovery.type", "single-node")
        .withEnv("xpack.security.enabled", "false")
        .withEnv("xpack.security.transport.ssl.enabled", "false")
        .withEnv("xpack.security.http.ssl.enabled", "false");

    elasticSearch.start();

    log.info("*** Starting elasticsearch at address: {}", elasticSearch.getHttpHostAddress());

//    // setup of solr (without same config as in docker-compose)
//    solr = new SolrContainer(DockerImageName.parse("solr:9.2.1"));
//    // deactivate zookeeper for testing (leads to some bugs in the testcontainers)
//    solr.withZookeeper(false);
//    // copy the solr config to the container (later on we can use them via exec in container)
//    solr.withCopyToContainer(
//        MountableFile.forHostPath(
//            new File("docker/apps/solr/solr").getAbsolutePath()
//        ), "/gams_config"
//    );
//
//    // .withCommand seems not to work with the solr container
//    // solr.withCommand("solr-precreate hupfi");
//
//    // Add appropriate wait strategy
//    // Use a more reliable wait strategy - waiting for the HTTP endpoint
//    solr.waitingFor(Wait.forHttp("/solr/admin/cores?action=STATUS")
//        .forPort(8983)
//        // TODO bit long interrupt time!
//        .withStartupTimeout(Duration.of(90, ChronoUnit.SECONDS)));
//
//    try {
//      solr.start();
//      // create the expected base cores
//      solr.execInContainer("bash", "bin/solr", "create_core", "-c", IntegrationTest.SOLR_TEST_CORE, "-d", "/gams_config/data/configsets/base");
//      solr.execInContainer("bash", "bin/solr", "create_core", "-c", IntegrationTest.SOLR_GAMS_CORE, "-d", "/gams_config/data/gams");
//      // make sure that configuration stuff is available (like the configsets)
//      solr.execInContainer("cp", "-r","/gams_config/data", "/var/solr");
//    } catch (Exception e) {
//      String msg = String.format("Solr didn't start correctly for testing. Exec in container didn't work. Got solr logs: %s Got exception: %s", solr.getLogs(), e);
//      log.error(msg);
//      throw new AssertionError(msg);
//    }

  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    // dynamic postgres configuration
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);

    // dynamic elasticsearch configuration
    registry.add("gams.docker.elasticsearchUrl", () -> String.format("http://%s", elasticSearch.getHttpHostAddress()));

    // set solr host and port
//    registry.add("gams.docker.baseSearchUrl", () -> String.format("""
//        http://%s:%s""", solr.getHost(), solr.getSolrPort()));
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
  public void tearDown() throws InterruptedException {
    eventCaptureListener.clearEvents();
    datastreamContentRepository.deleteAll();
    dublinCoreElementRepository.deleteAll();
    datastreamRepository.deleteAll();
    collectionRepository.deleteAll();
    digitalObjectRepository.deleteAll();
    projectRepository.deleteAll();

    // solr
    // gams core is being filled asynchronously -> so wiping will fail here
    //solrClient.wipeCore(IntegrationTest.SOLR_GAMS_CORE);
    // solrClient.wipeCore(IntegrationTest.SOLR_TEST_CORE);
  }


}
