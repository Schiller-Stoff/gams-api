package org.zim.gamsapi.application.Integration.BaseSearch;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import org.zim.gamsapi.IntegrationTest;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Handles integration of apache solr for testing.
 */
@Slf4j
public class BaseSearchIntegrationTest extends IntegrationTest {

  @Autowired
  SOLRClient solrClient;

  public static final String SOLR_TEST_CORE = "test";

  public static final String SOLR_GAMS_CORE = "gams";

  static final SolrContainer solr;

  static {
    // setup of solr (without same config as in docker-compose)
    // TODO hardcoded version
    solr = new SolrContainer(DockerImageName.parse("solr:9.2.1"));
    // deactivate zookeeper for testing (leads to some bugs in the testcontainers)
    solr.withZookeeper(false);
    // copy the solr config to the container (later on we can use them via exec in container)
    solr.withCopyToContainer(
        MountableFile.forHostPath(
            new File("docker/apps/solr/solr").getAbsolutePath()
        ), "/gams_config"
    );

    // .withCommand seems not to work with the solr container
    // solr.withCommand("solr-precreate hupfi");

    // Add appropriate wait strategy
    // Use a more reliable wait strategy - waiting for the HTTP endpoint
    solr.waitingFor(Wait.forHttp("/solr/admin/cores?action=STATUS")
        // TODO hardccoded port?
        .forPort(8983)
        // TODO bit long interrupt time!
        .withStartupTimeout(Duration.of(90, ChronoUnit.SECONDS)));

    try {
      solr.start();
      // create the expected base cores
      solr.execInContainer("bash", "bin/solr", "create_core", "-c", BaseSearchIntegrationTest.SOLR_TEST_CORE, "-d", "/gams_config/data/configsets/base");
      solr.execInContainer("bash", "bin/solr", "create_core", "-c", BaseSearchIntegrationTest.SOLR_GAMS_CORE, "-d", "/gams_config/data/gams");
      // make sure that configuration stuff is available (like the configsets)
      solr.execInContainer("cp", "-r","/gams_config/data", "/var/solr");
    } catch (Exception e) {
      String msg = String.format("Solr didn't start correctly for testing. Exec in container didn't work. Got solr logs: %s Got exception: %s", solr.getLogs(), e);
      log.error(msg);
      throw new AssertionError(msg);
    }
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    // set solr host and port
    registry.add("gams.docker.baseSearchUrl", () -> String.format("""
        http://%s:%s""", solr.getHost(), solr.getSolrPort()));
  }


  @AfterEach
  public void tearDown(){
    // solr
    // gams core is being filled asynchronously? -> so wiping will fail here
    solrClient.wipeCore(BaseSearchIntegrationTest.SOLR_GAMS_CORE);
    solrClient.wipeCore(BaseSearchServiceIT.SOLR_TEST_CORE);
  }

}
