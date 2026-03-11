package org.ddh.gamsapi.application.WebDeployment;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.application.WebDeployment.dto.WebDeploymentInfo;
import org.ddh.gamsapi.application.WebDeployment.exceptions.WebDeploymentNotFoundException;
import org.ddh.gamsapi.domain.Project.ProjectBuilder;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectNotFoundException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.ddh.gamsapi.infrastructure.System.configproperties.GAMSStorageProperties;
import org.ddh.gamsapi.infrastructure.System.security.IUserPrincipalAuditorMapping;
import org.ddh.gamsapi.infrastructure.System.security.exceptions.UserAuthenticationRequiredException;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Integration tests for {@link WebDeploymentService}.
 *
 * <p>Tests the full deployment lifecycle against a real PostgreSQL database
 * (via Testcontainers) and real filesystem operations. Uses the Spring-configured
 * {@code gams.storage.webRootPath} property (defaults to {@code gams-web-test}
 * in the test profile).
 *
 * <p>Covers: deploy, getDeploymentInfo, undeploy — including filesystem verification,
 * database record verification, redeployment (overwrite), and error conditions.
 *
 * <p><b>Note:</b> Once {@code WebDeploymentRepository} is registered in
 * {@link org.ddh.gamsapi.TestUtilities.TestCleanupService}, the DB cleanup
 * will be handled automatically by the parent's {@code tearDown()}.
 * Until then, this test class cleans up its own DB records.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WebDeploymentServiceIT extends IntegrationTest {

  // ==================================================================================
  // Dependencies
  // ==================================================================================

  @MockitoBean
  private AuditingHandler auditingHandler;

  @MockitoBean
  private IUserPrincipalAuditorMapping userPrincipalAuditorMapping;

  @Autowired
  private WebDeploymentService webDeploymentService;

  @Autowired
  private WebDeploymentRepository webDeploymentRepository;

  @Autowired
  private WebDeploymentContentRepository webDeploymentContentRepository;

  @Autowired
  private IProjectRepository projectRepository;

  @Autowired
  private GAMSStorageProperties gamsStorageProperties;

  /** Resolved absolute path to the web root directory. */
  private Path webRoot;

  // ==================================================================================
  // Setup / Cleanup
  // ==================================================================================

  @BeforeEach
  public void setup() {
    webRoot = Paths.get(gamsStorageProperties.getWebRootPath()).toAbsolutePath();

    Mockito.when(userPrincipalAuditorMapping.getCurrentAuditor())
        .thenReturn(Optional.of("test-user"));
    projectRepository.save(
        ProjectBuilder.builder()
            .projectAbbr(TestProject.PROJECT_ABBR.getValue())
            .build()
    );
  }

  /**
   * DB cleanup is handled by the parent class (TestCleanupService).
   * We additionally clean up web deployment DB records (until TestCleanupService
   * is updated) and any filesystem artifacts under the project's web directory.
   */
  @Override
  @AfterEach
  public void tearDown() {
    // Filesystem cleanup: remove the test project's web directory
    cleanupProjectWebDir(TestProject.PROJECT_ABBR.getValue());
    // DB cleanup: remove web deployment record
    webDeploymentRepository.deleteAll();
    super.tearDown();
  }

  /**
   * Deletes the given project's web deployment directory and all its contents.
   * Best-effort — logs warnings but does not throw.
   */
  private void cleanupProjectWebDir(String projectAbbr) {
    Path projectDir = webRoot.resolve(projectAbbr);
    if (!Files.exists(projectDir)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(projectDir)) {
      paths.sorted(Comparator.reverseOrder())
          .forEach(p -> {
            try {
              Files.delete(p);
            } catch (IOException e) {
              // best effort cleanup
            }
          });
    } catch (IOException e) {
      // best effort cleanup
    }
  }


  // ==================================================================================
  // deploy()
  // ==================================================================================

  @Nested
  @DisplayName("deploy()")
  public class Deploy {

    @Test
    public void throwsProjectNotFoundExceptionWhenProjectDoesNotExist() {
      InputStream zipStream = createMinimalSiteZip();

      Assertions.assertThatThrownBy(
          () -> webDeploymentService.deploy("nonexist", zipStream)
      ).isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    public void throwsUserAuthenticationRequiredExceptionWhenNotLoggedIn() {
      Mockito.when(userPrincipalAuditorMapping.getCurrentAuditor())
          .thenReturn(Optional.empty());

      InputStream zipStream = createMinimalSiteZip();

      Assertions.assertThatThrownBy(
          () -> webDeploymentService.deploy(TestProject.PROJECT_ABBR.getValue(), zipStream)
      ).isInstanceOf(UserAuthenticationRequiredException.class);
    }

    @Test
    public void createsExpectedFilesystemStructure() {
      InputStream zipStream = createFullSiteZip();

      webDeploymentService.deploy(TestProject.PROJECT_ABBR.getValue(), zipStream);

      Path projectDir = webRoot.resolve(TestProject.PROJECT_ABBR.getValue());
      Assertions.assertThat(Files.isDirectory(projectDir)).isTrue();
      Assertions.assertThat(Files.exists(projectDir.resolve("index.html"))).isTrue();
      Assertions.assertThat(Files.exists(projectDir.resolve("css/styles.css"))).isTrue();
      Assertions.assertThat(Files.exists(projectDir.resolve("js/app.js"))).isTrue();
    }

    @Test
    public void createsExpectedFileContent() throws IOException {
      InputStream zipStream = createMinimalSiteZip();

      webDeploymentService.deploy(TestProject.PROJECT_ABBR.getValue(), zipStream);

      Path indexHtml = webRoot
          .resolve(TestProject.PROJECT_ABBR.getValue())
          .resolve("index.html");
      String content = Files.readString(indexHtml);
      Assertions.assertThat(content).isEqualTo("<html><body>Hello</body></html>");
    }

    @Test
    public void persistsDeploymentMetadataInDatabase() {
      InputStream zipStream = createMinimalSiteZip();

      WebDeploymentInfo result = webDeploymentService.deploy(
          TestProject.PROJECT_ABBR.getValue(), zipStream);

      Assertions.assertThat(result).isNotNull();
      Assertions.assertThat(result.projectAbbr())
          .isEqualTo(TestProject.PROJECT_ABBR.getValue());
      Assertions.assertThat(result.deployedBy()).isEqualTo("test-user");
      Assertions.assertThat(result.fileCount()).isEqualTo(1);
      Assertions.assertThat(result.totalSize()).isGreaterThan(0);
      Assertions.assertThat(result.deployedAt()).isNotNull();

      // Verify DB record directly
      Optional<WebDeployment> dbRecord = webDeploymentRepository.findById(
          TestProject.PROJECT_ABBR.getValue());
      Assertions.assertThat(dbRecord).isPresent();
      Assertions.assertThat(dbRecord.get().getDeployedBy()).isEqualTo("test-user");
      Assertions.assertThat(dbRecord.get().getFileCount()).isEqualTo(1);
    }

    @Test
    public void redeploymentReplacesFilesystemContentCompletely() throws IOException {
      // First deployment: index.html
      webDeploymentService.deploy(
          TestProject.PROJECT_ABBR.getValue(), createMinimalSiteZip());

      Path projectDir = webRoot.resolve(TestProject.PROJECT_ABBR.getValue());
      Assertions.assertThat(Files.exists(projectDir.resolve("index.html"))).isTrue();

      // Second deployment: different structure (css/styles.css only, no index.html)
      byte[] secondZip = createZipWithEntries(
          new ZipEntryData("css/styles.css", "body { color: red; }")
      );
      webDeploymentService.deploy(
          TestProject.PROJECT_ABBR.getValue(), new ByteArrayInputStream(secondZip));

      // Old files must be gone — atomic swap guarantees clean replacement
      Assertions.assertThat(Files.exists(projectDir.resolve("index.html"))).isFalse();
      // New files must exist
      Assertions.assertThat(Files.exists(projectDir.resolve("css/styles.css"))).isTrue();
      Assertions.assertThat(Files.readString(projectDir.resolve("css/styles.css")))
          .isEqualTo("body { color: red; }");
    }

    @Test
    public void redeploymentUpdatesExistingDatabaseRecord() {
      // First deployment
      WebDeploymentInfo first = webDeploymentService.deploy(
          TestProject.PROJECT_ABBR.getValue(), createMinimalSiteZip());
      Instant firstDeployedAt = first.deployedAt();

      // Small delay to ensure timestamps differ
      try { Thread.sleep(50); } catch (InterruptedException ignored) {}

      // Second deployment with different user
      Mockito.when(userPrincipalAuditorMapping.getCurrentAuditor())
          .thenReturn(Optional.of("second-user"));

      byte[] secondZip = createZipWithEntries(
          new ZipEntryData("page1.html", "<html/>"),
          new ZipEntryData("page2.html", "<html/>")
      );
      WebDeploymentInfo second = webDeploymentService.deploy(
          TestProject.PROJECT_ABBR.getValue(), new ByteArrayInputStream(secondZip));

      Assertions.assertThat(second.deployedBy()).isEqualTo("second-user");
      Assertions.assertThat(second.fileCount()).isEqualTo(2);
      Assertions.assertThat(second.deployedAt()).isAfter(firstDeployedAt);

      // Only one DB record should exist (upsert, not insert)
      Assertions.assertThat(webDeploymentRepository.count()).isEqualTo(1);
    }
  }

  // ==================================================================================
  // getDeploymentInfo()
  // ==================================================================================

  @Nested
  @DisplayName("getDeploymentInfo()")
  public class GetDeploymentInfo {

    @Test
    public void throwsProjectNotFoundExceptionWhenProjectDoesNotExist() {
      Assertions.assertThatThrownBy(
          () -> webDeploymentService.getDeploymentInfo("nonexist")
      ).isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    public void throwsWebDeploymentNotFoundExceptionWhenNoDeploymentExists() {
      Assertions.assertThatThrownBy(
          () -> webDeploymentService.getDeploymentInfo(
              TestProject.PROJECT_ABBR.getValue())
      ).isInstanceOf(WebDeploymentNotFoundException.class);
    }

    @Test
    public void returnsExpectedMetadataAfterDeploy() {
      webDeploymentService.deploy(
          TestProject.PROJECT_ABBR.getValue(), createFullSiteZip());

      WebDeploymentInfo info = webDeploymentService.getDeploymentInfo(
          TestProject.PROJECT_ABBR.getValue());

      Assertions.assertThat(info.projectAbbr())
          .isEqualTo(TestProject.PROJECT_ABBR.getValue());
      Assertions.assertThat(info.deployedBy()).isEqualTo("test-user");
      // Full site zip has 3 files: index.html, css/styles.css, js/app.js
      Assertions.assertThat(info.fileCount()).isEqualTo(3);
      Assertions.assertThat(info.totalSize()).isGreaterThan(0);
      Assertions.assertThat(info.deployedAt()).isBefore(Instant.now());
    }
  }

  // ==================================================================================
  // undeploy()
  // ==================================================================================

  @Nested
  @DisplayName("undeploy()")
  public class Undeploy {

    @Test
    public void throwsProjectNotFoundExceptionWhenProjectDoesNotExist() {
      Assertions.assertThatThrownBy(
          () -> webDeploymentService.undeploy("nonexist")
      ).isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    public void throwsWebDeploymentNotFoundExceptionWhenNoDeploymentExists() {
      Assertions.assertThatThrownBy(
          () -> webDeploymentService.undeploy(TestProject.PROJECT_ABBR.getValue())
      ).isInstanceOf(WebDeploymentNotFoundException.class);
    }

    @Test
    public void removesFilesystemContent() {
      webDeploymentService.deploy(
          TestProject.PROJECT_ABBR.getValue(), createMinimalSiteZip());

      Path projectDir = webRoot.resolve(TestProject.PROJECT_ABBR.getValue());
      Assertions.assertThat(Files.isDirectory(projectDir)).isTrue();

      webDeploymentService.undeploy(TestProject.PROJECT_ABBR.getValue());

      Assertions.assertThat(Files.exists(projectDir)).isFalse();
    }

    @Test
    public void removesDatabaseRecord() {
      webDeploymentService.deploy(
          TestProject.PROJECT_ABBR.getValue(), createMinimalSiteZip());

      Assertions.assertThat(
          webDeploymentRepository.findById(TestProject.PROJECT_ABBR.getValue())
      ).isPresent();

      webDeploymentService.undeploy(TestProject.PROJECT_ABBR.getValue());

      Assertions.assertThat(
          webDeploymentRepository.findById(TestProject.PROJECT_ABBR.getValue())
      ).isEmpty();
    }

    @Test
    public void allowsRedeployAfterUndeploy() {
      // Deploy
      webDeploymentService.deploy(
          TestProject.PROJECT_ABBR.getValue(), createMinimalSiteZip());

      // Undeploy
      webDeploymentService.undeploy(TestProject.PROJECT_ABBR.getValue());

      // Redeploy should succeed
      WebDeploymentInfo info = webDeploymentService.deploy(
          TestProject.PROJECT_ABBR.getValue(), createMinimalSiteZip());

      Assertions.assertThat(info).isNotNull();
      Assertions.assertThat(info.fileCount()).isEqualTo(1);

      Path projectDir = webRoot.resolve(TestProject.PROJECT_ABBR.getValue());
      Assertions.assertThat(Files.exists(projectDir.resolve("index.html"))).isTrue();
    }
  }

  // ==================================================================================
  // Full lifecycle
  // ==================================================================================

  @Nested
  @DisplayName("Full deployment lifecycle")
  public class FullLifecycle {

    @Test
    public void deployThenQueryThenRedeployThenUndeploy() {
      // Step 1: Deploy
      WebDeploymentInfo deployed = webDeploymentService.deploy(
          TestProject.PROJECT_ABBR.getValue(), createMinimalSiteZip());
      Assertions.assertThat(deployed.fileCount()).isEqualTo(1);

      // Step 2: Query
      WebDeploymentInfo queried = webDeploymentService.getDeploymentInfo(
          TestProject.PROJECT_ABBR.getValue());
      // PostgreSQL timestamp(6) truncates nanoseconds — compare at microsecond precision
      Assertions.assertThat(queried.deployedAt())
          .isCloseTo(deployed.deployedAt(), Assertions.within(1, java.time.temporal.ChronoUnit.MICROS));

      // Step 3: Redeploy with more files
      byte[] biggerZip = createZipWithEntries(
          new ZipEntryData("index.html", "<html>v2</html>"),
          new ZipEntryData("about.html", "<html>About</html>"),
          new ZipEntryData("assets/logo.svg", "<svg/>")
      );
      WebDeploymentInfo redeployed = webDeploymentService.deploy(
          TestProject.PROJECT_ABBR.getValue(), new ByteArrayInputStream(biggerZip));
      Assertions.assertThat(redeployed.fileCount()).isEqualTo(3);
      Assertions.assertThat(redeployed.deployedAt()).isAfter(deployed.deployedAt());

      // Step 4: Undeploy
      webDeploymentService.undeploy(TestProject.PROJECT_ABBR.getValue());

      // Step 5: Verify clean state
      Assertions.assertThatThrownBy(
          () -> webDeploymentService.getDeploymentInfo(
              TestProject.PROJECT_ABBR.getValue())
      ).isInstanceOf(WebDeploymentNotFoundException.class);

      Path projectDir = webRoot.resolve(TestProject.PROJECT_ABBR.getValue());
      Assertions.assertThat(Files.exists(projectDir)).isFalse();

      Assertions.assertThat(webDeploymentRepository.count()).isEqualTo(0);
    }
  }


  // ==================================================================================
  // Zip creation helpers
  // ==================================================================================

  private record ZipEntryData(String path, String content) {}

  private InputStream createMinimalSiteZip() {
    byte[] zip = createZipWithEntries(
        new ZipEntryData("index.html", "<html><body>Hello</body></html>")
    );
    return new ByteArrayInputStream(zip);
  }

  private InputStream createFullSiteZip() {
    byte[] zip = createZipWithEntries(
        new ZipEntryData("index.html", "<!DOCTYPE html><html><body>Main</body></html>"),
        new ZipEntryData("css/styles.css", "body { margin: 0; }"),
        new ZipEntryData("js/app.js", "console.log('init');")
    );
    return new ByteArrayInputStream(zip);
  }

  private static byte[] createZipWithEntries(ZipEntryData... entries) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
      for (ZipEntryData entry : entries) {
        zos.putNextEntry(new ZipEntry(entry.path()));
        zos.write(entry.content().getBytes());
        zos.closeEntry();
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to create test zip", e);
    }
    return baos.toByteArray();
  }
}