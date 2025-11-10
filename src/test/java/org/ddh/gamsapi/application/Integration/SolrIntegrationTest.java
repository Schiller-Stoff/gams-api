package org.ddh.gamsapi.application.Integration;

import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrClient;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrGamsCores;
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
 *
 * CRITICAL CHANGES:
 * 1. Mounts BOTH base and custom-fulltext configsets
 * 2. Creates FULLTEXT_CORE with custom-fulltext configuration
 * 3. Creates GAMS_CORE and TEST_CORE with base configuration
 */
@Slf4j
public class SolrIntegrationTest extends IntegrationTest {

  @Autowired
  public SolrClient solrClient;

  static final SolrContainer solr;

  static {
    // ========================================
    // VALIDATE CONFIGSET EXISTENCE
    // ========================================
    // base configuration for every solr to be created
    String baseConfigsetPath = "docker/apps/solr/solr/data/configsets/base";

    // base search configuration
    final String BASE_SEARCH_SERVICE_NAME = "gams";
    final String BASE_SEARCH_CONTAINER_TEMP_PATH = String.format("/tmp/%s_configset", BASE_SEARCH_SERVICE_NAME);
    String baseSearchConfigPath = "docker/apps/solr/solr/data/gams/conf";

    // custom-search configuration
    String customSearchConfigPath = "docker/apps/solr/solr/data/custom-search/conf";

    File baseConfigset = new File(baseConfigsetPath);
    File baseSearchConfig = new File(baseSearchConfigPath);
    File customSearchConfig = new File(customSearchConfigPath);

    if (!baseConfigset.exists()) {
      throw new AssertionError("'base' configset not found at: " + baseConfigset.getAbsolutePath());
    }

    if (!baseSearchConfig.exists()) {
      throw new AssertionError("'custom-search' config not found at: " + customSearchConfig.getAbsolutePath());
    }

    if (!customSearchConfig.exists()) {
      throw new AssertionError("'custom-search' config not found at: " + customSearchConfig.getAbsolutePath());
    }

    log.info("✓ Found 'base' configset at: {}", baseConfigset.getAbsolutePath());
    log.info("✓ Found {} configset at: {}", BASE_SEARCH_SERVICE_NAME,  baseConfigset.getAbsolutePath());
    log.info("✓ Found 'custom-search' config at: {}", customSearchConfig.getAbsolutePath());

    // ========================================
    // CONTAINER SETUP WITH BOTH CONFIGSETS
    // ========================================
    solr = new SolrContainer(DockerImageName.parse("solr:9.2.1"))
        .withZookeeper(false)
        // Mount base configset
        .withCopyToContainer(
            MountableFile.forHostPath(baseConfigsetPath),
            "/tmp/base_configset"
        )
        .withCopyToContainer(
            MountableFile.forHostPath(baseSearchConfigPath),
            BASE_SEARCH_CONTAINER_TEMP_PATH
        )
        // Mount custom-search configset
        .withCopyToContainer(
            MountableFile.forHostPath(customSearchConfigPath),
            "/tmp/custom_search_configset"
        )
        .waitingFor(Wait.forHttp("/solr/admin/cores?action=STATUS")
            .forPort(8983)
            .forStatusCode(200)
            .withStartupTimeout(Duration.ofSeconds(90)));

    try {
      log.info("Starting Solr container...");
      solr.start();
      log.info("✓ Solr started at {}:{}", solr.getHost(), solr.getSolrPort());

      // ========================================
      // COPY CONFIGSETS TO SOLR DATA DIR
      // ========================================
      log.info("Copying configsets to /var/solr/data/configsets for API access...");
      solr.execInContainer("mkdir", "-p", "/var/solr/data/configsets");

      // Copy base configset
      var copyBaseForApi = solr.execInContainer(
          "sh", "-c",
          "cp -r /tmp/base_configset /var/solr/data/configsets/base && chmod -R 755 /var/solr/data/configsets/base"
      );

      if (copyBaseForApi.getExitCode() != 0) {
        log.warn("Failed to copy base configset: {}", copyBaseForApi.getStderr());
      } else {
        log.info("✓ Base configset copied to /var/solr/data/configsets/base");
      }

      var copyBaseSearchForApi = solr.execInContainer(
          "sh", "-c",
          String.format("cp -r %s /var/solr/data/configsets/%s && chmod -R 755 /var/solr/data/configsets/%s",
              BASE_SEARCH_CONTAINER_TEMP_PATH, BASE_SEARCH_SERVICE_NAME, BASE_SEARCH_SERVICE_NAME)
      );

      if (copyBaseSearchForApi.getExitCode() != 0) {
        log.warn("Failed to copy {} configset: {}", BASE_SEARCH_SERVICE_NAME, copyBaseSearchForApi.getStderr());
      } else {
        log.info("✓ {} configset copied to /var/solr/data/configsets/{}",
            BASE_SEARCH_SERVICE_NAME, BASE_SEARCH_SERVICE_NAME);
      }

      // Copy custom-search configset
      var copyCustomForApi = solr.execInContainer(
          "sh", "-c",
          "cp -r /tmp/custom_search_configset /var/solr/data/configsets/custom-search && chmod -R 755 /var/solr/data/configsets/custom-search"
      );

      if (copyCustomForApi.getExitCode() != 0) {
        log.warn("Failed to copy custom-search configset: {}", copyCustomForApi.getStderr());
      } else {
        log.info("✓ Custom-search configset copied to /var/solr/data/configsets/custom-search");
      }

      Thread.sleep(2000);

      // ========================================
      // CREATE CORES WITH CORRECT CONFIGSETS
      // ========================================

      // 1. TEST_CORE (uses base configset)
      log.info("Creating '{}' core with BASE configset...", SolrGamsCores.TEST_CORE.value);
      var testCore = solr.execInContainer(
          "solr", "create_core", "-c", SolrGamsCores.TEST_CORE.value, "-d", "/tmp/base_configset"
      );
      log.info("Test core - exitCode: {}, stdout: '{}'",
          testCore.getExitCode(), testCore.getStdout().trim());

      if (testCore.getExitCode() != 0) {
        throw new AssertionError("Failed to create test core. Exit: " + testCore.getExitCode() +
            " stdout: " + testCore.getStdout());
      }

      // 2. GAMS_CORE (uses base configset)
      // TODO rename / redo to base-search core!
      log.info("Creating '{}' core with BASE configset...", SolrGamsCores.GAMS_CORE.value);
      var gamsCore = solr.execInContainer(
          "solr", "create_core", "-c", SolrGamsCores.GAMS_CORE.value, "-d", BASE_SEARCH_CONTAINER_TEMP_PATH
      );
      log.info("GAMS core - exitCode: {}, stdout: '{}'",
          gamsCore.getExitCode(), gamsCore.getStdout().trim());

      if (gamsCore.getExitCode() != 0) {
        throw new AssertionError("Failed to create GAMS core. Exit: " + gamsCore.getExitCode() +
            " stdout: " + gamsCore.getStdout());
      }

      // 3. CUSTOM_SEARCH_CORE (uses CUSTOM-SEARCH configset) ← THE KEY CHANGE
      log.info("Creating '{}' core with CUSTOM-SEARCH configset...", SolrGamsCores.CUSTOM_SEARCH_CORE.value);
      var customSearchCore = solr.execInContainer(
          "solr", "create_core", "-c", SolrGamsCores.CUSTOM_SEARCH_CORE.value,
          "-d", "/tmp/custom_search_configset"  // ← USES CUSTOM CONFIG
      );
      log.info("Custom-search core - exitCode: {}, stdout: '{}'",
          customSearchCore.getExitCode(), customSearchCore.getStdout().trim());

      if (customSearchCore.getExitCode() != 0) {
        throw new AssertionError("Failed to create custom-search core. Exit: " + customSearchCore.getExitCode() +
            " stdout: " + customSearchCore.getStdout());
      }

      Thread.sleep(1000);
      log.info("✓ Solr test container fully initialized with 3 cores");
      log.info("  - {} (base schema): objectFulltext, dc.* fields", SolrGamsCores.TEST_CORE.value);
      log.info("  - {} (base schema): objectFulltext, dc.* fields", SolrGamsCores.GAMS_CORE.value);
      log.info("  - {} (custom-search schema): entityFulltext, entity* fields", SolrGamsCores.CUSTOM_SEARCH_CORE.value);

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
      solrClient.wipeCore(SolrGamsCores.CUSTOM_SEARCH_CORE.value);
      super.tearDown();
    } catch (Exception e) {
      log.warn("Failed to wipe cores in tearDown: {}", e.getMessage());
    }
  }
}