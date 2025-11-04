package org.ddh.gamsapi.application.Integration.BaseSearch;

import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrClient;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrGamsCores;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.time.Duration;

/**
 * Handles integration of apache solr for testing.
 */
@Slf4j
public class BaseSearchIntegrationTest extends IntegrationTest {

  @Autowired
  SolrClient solrClient;

  static final SolrContainer solr;

  static {
    String baseConfigsetPath = "docker/apps/solr/solr/data/configsets/base";
    File baseConfigset = new File(baseConfigsetPath);

    if (!baseConfigset.exists()) {
      throw new AssertionError("'base' configset not found at: " + baseConfigset.getAbsolutePath());
    }

    log.info("✓ Found 'base' configset at: {}", baseConfigset.getAbsolutePath());

    solr = new SolrContainer(DockerImageName.parse("solr:9.2.1"))
        .withZookeeper(false)
        .withCopyToContainer(
            MountableFile.forHostPath(baseConfigsetPath),
            "/tmp/base_configset"
        )
        .waitingFor(Wait.forHttp("/solr/admin/cores?action=STATUS")
            .forPort(8983)
            .forStatusCode(200)
            .withStartupTimeout(Duration.ofSeconds(90)));

    try {
      log.info("Starting Solr container...");
      solr.start();
      log.info("✓ Solr started at {}:{}", solr.getHost(), solr.getSolrPort());

      // Copy to /var/solr/data/configsets for API usage
      log.info("Copying configset to /var/solr/data/configsets for API access...");
      solr.execInContainer("mkdir", "-p", "/var/solr/data/configsets");

      var copyForApi = solr.execInContainer(
          "sh", "-c",
          "cp -r /tmp/base_configset /var/solr/data/configsets/base && chmod -R 755 /var/solr/data/configsets/base"
      );

      if (copyForApi.getExitCode() != 0) {
        log.warn("Failed to copy to /var/solr/data/configsets (API won't work): {}", copyForApi.getStderr());
      } else {
        log.info("✓ Configset copied to /var/solr/data/configsets/base for API usage");
      }

      Thread.sleep(2000);

      // ========================================
      // CREATE ALL THREE CORES
      // ========================================

      // 1. TEST_CORE
      log.info("Creating '{}' core...", SolrGamsCores.TEST_CORE.value);
      var testCore = solr.execInContainer(
          "solr", "create_core", "-c", SolrGamsCores.TEST_CORE.value, "-d", "/tmp/base_configset"
      );
      log.info("Test core - exitCode: {}, stdout: '{}'",
          testCore.getExitCode(), testCore.getStdout().trim());

      if (testCore.getExitCode() != 0) {
        throw new AssertionError("Failed to create test core. Exit: " + testCore.getExitCode() + " stdout: " + testCore.getStdout());
      }

      // 2. GAMS_CORE
      log.info("Creating '{}' core...", SolrGamsCores.GAMS_CORE.value);
      var gamsCore = solr.execInContainer(
          "solr", "create_core", "-c", SolrGamsCores.GAMS_CORE.value, "-d", "/tmp/base_configset"
      );
      log.info("GAMS core - exitCode: {}, stdout: '{}'",
          gamsCore.getExitCode(), gamsCore.getStdout().trim());

      if (gamsCore.getExitCode() != 0) {
        throw new AssertionError("Failed to create GAMS core. Exit: " + gamsCore.getExitCode() + " stdout: " + gamsCore.getStdout());
      }

      // 3. ADD FULLTEXT_CORE
      log.info("Creating '{}' core...", SolrGamsCores.FULLTEXT_CORE.value);
      var fulltextCore = solr.execInContainer(
          "solr", "create_core", "-c", SolrGamsCores.FULLTEXT_CORE.value, "-d", "/tmp/base_configset"
      );
      log.info("Fulltext core - exitCode: {}, stdout: '{}'",
          fulltextCore.getExitCode(), fulltextCore.getStdout().trim());

      if (fulltextCore.getExitCode() != 0) {
        throw new AssertionError("Failed to create fulltext core. Exit: " + fulltextCore.getExitCode() + " stdout: " + fulltextCore.getStdout());
      }

      Thread.sleep(1000);
      log.info("✓ Solr test container fully initialized with 3 cores");

    } catch (Exception e) {
      log.error("Failed to initialize Solr container", e);
      throw new RuntimeException("Solr initialization failed", e);
    }
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    String solrUrl = String.format("http://%s:%d", solr.getHost(), solr.getSolrPort());
    log.info("Configuring test Solr URL: {}", solrUrl);
    registry.add("gams.docker.baseSearchUrl", () -> solrUrl);
  }

  @AfterEach
  public void tearDown() {
    try {
      log.debug("Cleaning up Solr cores after test");
      solrClient.wipeCore(SolrGamsCores.GAMS_CORE.value);
      solrClient.wipeCore(SolrGamsCores.TEST_CORE.value);
      solrClient.wipeCore(SolrGamsCores.FULLTEXT_CORE.value);
      super.tearDown();
    } catch (Exception e) {
      log.warn("Failed to wipe cores in tearDown: {}", e.getMessage());
    }
  }
}
