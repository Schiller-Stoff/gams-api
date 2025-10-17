package org.ddh.gamsapi.application.Integration.BaseSearch;

import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.IntegrationTest;
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
  SOLRClient solrClient;

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

      // Rename schema file in temp
      log.info("Preparing configset...");
      solr.execInContainer(
          "sh", "-c",
          "if [ -f /tmp/base_configset/conf/managed-schema.xml ]; then " +
              "mv /tmp/base_configset/conf/managed-schema.xml /tmp/base_configset/conf/managed-schema; fi"
      );

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

      // Create cores using absolute path (CLI method)
      log.info("Creating '{}' core...", GamsSolrCores.TEST_CORE.value);
      var testCore = solr.execInContainer(
          "solr", "create_core", "-c", GamsSolrCores.TEST_CORE.value, "-d", "/tmp/base_configset"
      );
      log.info("Test core - exitCode: {}, stdout: '{}'",
          testCore.getExitCode(), testCore.getStdout().trim());

      if (testCore.getExitCode() != 0) {
        throw new AssertionError("Failed to create test core. Exit: " + testCore.getExitCode() + "stdout: " + testCore.getStdout());
      }

      log.info("Creating '{}' core...", GamsSolrCores.GAMS_CORE.value);
      var gamsCore = solr.execInContainer(
          "solr", "create_core", "-c", GamsSolrCores.GAMS_CORE.value, "-d", "/tmp/base_configset"
      );
      log.info("GAMS core - exitCode: {}, stdout: '{}'",
          gamsCore.getExitCode(), gamsCore.getStdout().trim());

      if (gamsCore.getExitCode() != 0) {
        throw new AssertionError("Failed to create GAMS core. Exit: " + gamsCore.getExitCode() + "stdout: " + testCore.getStdout());
      }

      Thread.sleep(1000);
      log.info("✓ Solr test container fully initialized");

    } catch (Exception e) {
      // ... existing error handling ...
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
      solrClient.wipeCore(GamsSolrCores.GAMS_CORE.value);
      solrClient.wipeCore(GamsSolrCores.TEST_CORE.value);
      // needs to be called - teardown in parent class is not called automatically
      super.tearDown();
    } catch (Exception e) {
      log.warn("Failed to wipe cores in tearDown: {}", e.getMessage());
    }
  }
}
