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

  public static final String SOLR_TEST_CORE = "test";
  public static final String SOLR_GAMS_CORE = "gams";

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

      // Check who we're running as
      var whoami = solr.execInContainer("whoami");
      log.info("Executing commands as user: {}", whoami.getStdout().trim());

      // Rename schema file in temp location (before we try to copy it)
      log.info("Preparing configset in temp location...");
      var renameResult = solr.execInContainer(
          "sh", "-c",
          "if [ -f /tmp/base_configset/conf/managed-schema.xml ]; then " +
              "mv /tmp/base_configset/conf/managed-schema.xml /tmp/base_configset/conf/managed-schema; fi"
      );
      log.info("Schema rename - exitCode: {}", renameResult.getExitCode());

      // Verify temp configset
      var verifyTemp = solr.execInContainer("ls", "-la", "/tmp/base_configset/conf");
      log.info("Temp configset conf:\n{}", verifyTemp.getStdout());

      // Instead of copying, just use the absolute path when creating cores
      log.info("✓ Using configset from /tmp/base_configset");

      Thread.sleep(2000);

      // Create test core using ABSOLUTE PATH to our temp configset
      log.info("Creating '{}' core with configset from /tmp/base_configset...", SOLR_TEST_CORE);
      var testCore = solr.execInContainer(
          "solr", "create_core", "-c", SOLR_TEST_CORE, "-d", "/tmp/base_configset"
      );
      log.info("Test core - exitCode: {}, stdout: '{}', stderr: '{}'",
          testCore.getExitCode(), testCore.getStdout().trim(), testCore.getStderr().trim());

      if (testCore.getExitCode() != 0) {
        throw new AssertionError(
            "Failed to create test core. Exit: " + testCore.getExitCode() +
                "\nStdout: " + testCore.getStdout() + "\nStderr: " + testCore.getStderr()
        );
      }

      // Create gams core using ABSOLUTE PATH
      log.info("Creating '{}' core with configset from /tmp/base_configset...", SOLR_GAMS_CORE);
      var gamsCore = solr.execInContainer(
          "solr", "create_core", "-c", SOLR_GAMS_CORE, "-d", "/tmp/base_configset"
      );
      log.info("GAMS core - exitCode: {}, stdout: '{}', stderr: '{}'",
          gamsCore.getExitCode(), gamsCore.getStdout().trim(), gamsCore.getStderr().trim());

      if (gamsCore.getExitCode() != 0) {
        throw new AssertionError("Failed to create GAMS core. Exit: " + gamsCore.getExitCode());
      }

      Thread.sleep(1000);
      log.info("✓ Solr test container fully initialized with both cores using temp configset");

    } catch (Exception e) {
      String msg = String.format("Solr init failed: %s\n\nLogs:\n%s", e.getMessage(), solr.getLogs());
      log.error(msg, e);
      throw new AssertionError(msg, e);
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
      solrClient.wipeCore(SOLR_GAMS_CORE);
      solrClient.wipeCore(SOLR_TEST_CORE);
    } catch (Exception e) {
      log.warn("Failed to wipe cores in tearDown: {}", e.getMessage());
    }
  }
}
