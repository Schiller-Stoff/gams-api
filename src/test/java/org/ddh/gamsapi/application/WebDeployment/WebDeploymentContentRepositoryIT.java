package org.ddh.gamsapi.application.WebDeployment;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.application.WebDeployment.exceptions.WebDeploymentStorageException;
import org.ddh.gamsapi.infrastructure.System.configproperties.GAMSStorageProperties;
import org.ddh.gamsapi.infrastructure.System.utils.FileUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Integration tests for {@link WebDeploymentContentRepository}.
 *
 * <p>Tests the filesystem operations (deploy, delete, exists, getProjectWebPath)
 * against the real filesystem using the Spring-configured {@code gams.storage.webRootPath}
 * (defaults to {@code gams-web-test} in the test profile).
 *
 * <p>Follows the same pattern as {@code DatastreamContentRepositoryIT}: each test
 * cleans up its own filesystem artifacts. No database involvement — this repository
 * is a pure filesystem abstraction.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebDeploymentContentRepositoryIT extends IntegrationTest {

  private static final String TEST_PROJECT = "webtest";

  @Autowired
  private WebDeploymentContentRepository webDeploymentContentRepository;

  @Autowired
  private GAMSStorageProperties gamsStorageProperties;

  private Path webRoot;

  @BeforeEach
  void setup() {
    webRoot = Paths.get(gamsStorageProperties.getWebRootPath()).toAbsolutePath();
  }

  @AfterEach
  void cleanup() throws IOException {
    FileUtils.emptyDirectory(webRoot.toFile(), Set.of("README.md"));
    super.tearDown();
  }

  @Nested
  @DisplayName("deploy()")
  class Deploy {

    @Test
    void createsExpectedDirectoryForProject() {
      InputStream zip = createZip(
          entry("index.html", "<html/>")
      );

      webDeploymentContentRepository.deploy(TEST_PROJECT, zip);

      Path projectDir = webRoot.resolve(TEST_PROJECT);
      Assertions.assertThat(Files.isDirectory(projectDir)).isTrue();
    }

    @Test
    void extractsSingleFileWithExpectedContent() throws IOException {
      final String EXPECTED_CONTENT = "<html><body>Hello World</body></html>";
      InputStream zip = createZip(
          entry("index.html", EXPECTED_CONTENT)
      );

      webDeploymentContentRepository.deploy(TEST_PROJECT, zip);

      Path indexHtml = webRoot.resolve(TEST_PROJECT).resolve("index.html");
      Assertions.assertThat(Files.exists(indexHtml)).isTrue();
      Assertions.assertThat(Files.readString(indexHtml)).isEqualTo(EXPECTED_CONTENT);
    }

    @Test
    void extractsNestedDirectoryStructure() {
      InputStream zip = createZip(
          entry("index.html", "<html/>"),
          entry("css/styles.css", "body {}"),
          entry("js/app.js", "console.log();"),
          entry("images/icons/logo.svg", "<svg/>")
      );

      webDeploymentContentRepository.deploy(TEST_PROJECT, zip);

      Path projectDir = webRoot.resolve(TEST_PROJECT);
      Assertions.assertThat(Files.exists(projectDir.resolve("index.html"))).isTrue();
      Assertions.assertThat(Files.exists(projectDir.resolve("css/styles.css"))).isTrue();
      Assertions.assertThat(Files.exists(projectDir.resolve("js/app.js"))).isTrue();
      Assertions.assertThat(Files.exists(projectDir.resolve("images/icons/logo.svg"))).isTrue();
    }

    @Test
    void returnsExpectedDeploymentStats() {
      final String FILE_1_CONTENT = "Hello";
      final String FILE_2_CONTENT = "World!";
      InputStream zip = createZip(
          entry("a.txt", FILE_1_CONTENT),
          entry("b.txt", FILE_2_CONTENT)
      );

      WebDeploymentContentRepository.DeploymentStats stats =
          webDeploymentContentRepository.deploy(TEST_PROJECT, zip);

      Assertions.assertThat(stats.fileCount()).isEqualTo(2);
      Assertions.assertThat(stats.totalSize())
          .isEqualTo(FILE_1_CONTENT.getBytes().length + FILE_2_CONTENT.getBytes().length);
    }

    @Test
    void directoryOnlyEntriesAreNotCountedAsFiles() {
      InputStream zip = createZipWithDirectoryEntry(
          "emptydir/",
          entry("file.txt", "content")
      );

      WebDeploymentContentRepository.DeploymentStats stats =
          webDeploymentContentRepository.deploy(TEST_PROJECT, zip);

      // Only the file counts, not the directory entry
      Assertions.assertThat(stats.fileCount()).isEqualTo(1);

      // But directory should exist on filesystem
      Assertions.assertThat(
          Files.isDirectory(webRoot.resolve(TEST_PROJECT).resolve("emptydir"))
      ).isTrue();
    }

    @Test
    void atomicSwapReplacesOldContentCompletely() throws IOException {
      // First deployment
      webDeploymentContentRepository.deploy(TEST_PROJECT, createZip(
          entry("old.html", "old content"),
          entry("shared.css", "v1")
      ));

      Path projectDir = webRoot.resolve(TEST_PROJECT);
      Assertions.assertThat(Files.exists(projectDir.resolve("old.html"))).isTrue();
      Assertions.assertThat(Files.exists(projectDir.resolve("shared.css"))).isTrue();

      // Second deployment — completely different files
      webDeploymentContentRepository.deploy(TEST_PROJECT, createZip(
          entry("new.html", "new content"),
          entry("shared.css", "v2")
      ));

      // Old file gone
      Assertions.assertThat(Files.exists(projectDir.resolve("old.html"))).isFalse();
      // New file present
      Assertions.assertThat(Files.exists(projectDir.resolve("new.html"))).isTrue();
      // Shared file has new content
      Assertions.assertThat(Files.readString(projectDir.resolve("shared.css")))
          .isEqualTo("v2");
    }

    @Test
    void noOrphanTempDirectoriesAfterSuccessfulDeploy() throws IOException {
      webDeploymentContentRepository.deploy(TEST_PROJECT, createZip(
          entry("index.html", "<html/>")
      ));

      // Check that no .tmp- or .old- directories remain in the web root
      try (Stream<Path> siblings = Files.list(webRoot)) {
        long orphanCount = siblings
            .filter(p -> {
              String name = p.getFileName().toString();
              return name.startsWith(".tmp-" + TEST_PROJECT)
                  || name.startsWith(".old-" + TEST_PROJECT);
            })
            .count();
        Assertions.assertThat(orphanCount).isZero();
      }
    }

    @Test
    void throwsOnEmptyZip() {
      byte[] emptyZip = createEmptyZip();
      InputStream zip = new ByteArrayInputStream(emptyZip);

      Assertions.assertThatThrownBy(
              () -> webDeploymentContentRepository.deploy(TEST_PROJECT, zip)
          ).isInstanceOf(WebDeploymentStorageException.class)
          .hasMessageContaining("no entries");
    }

    @Test
    void throwsOnPathTraversalAttack() {
      byte[] maliciousZip = createMaliciousZip("../../etc/passwd", "malicious");

      var bos = new ByteArrayInputStream(maliciousZip);

      Assertions.assertThatThrownBy(
              () -> webDeploymentContentRepository.deploy(
                  TEST_PROJECT, bos)
          ).isInstanceOf(WebDeploymentStorageException.class)
          .hasMessageContaining("path traversal");
    }

    @Test
    void cleansUpTempDirectoryOnPathTraversalFailure() throws IOException {
      byte[] maliciousZip = createMaliciousZip("../../escape.txt", "data");

      try {
        webDeploymentContentRepository.deploy(
            TEST_PROJECT, new ByteArrayInputStream(maliciousZip));
      } catch (WebDeploymentStorageException _) {
        // expected
      }

      // No temp directories should remain
      try (Stream<Path> siblings = Files.list(webRoot)) {
        long tmpCount = siblings
            .filter(p -> p.getFileName().toString().startsWith(".tmp-" + TEST_PROJECT))
            .count();
        Assertions.assertThat(tmpCount).isZero();
      }
    }
  }

  @Nested
  @DisplayName("delete()")
  class Delete {

    @Test
    void returnsTrueAndRemovesDirectoryWhenDeploymentExists() {
      webDeploymentContentRepository.deploy(TEST_PROJECT, createZip(
          entry("index.html", "<html/>"),
          entry("css/styles.css", "body {}")
      ));

      Path projectDir = webRoot.resolve(TEST_PROJECT);
      Assertions.assertThat(Files.isDirectory(projectDir)).isTrue();

      boolean result = webDeploymentContentRepository.delete(TEST_PROJECT);

      Assertions.assertThat(result).isTrue();
      Assertions.assertThat(Files.exists(projectDir)).isFalse();
    }

    @Test
    void returnsFalseWhenNoDeploymentExists() {
      boolean result = webDeploymentContentRepository.delete("neverdeployed");

      Assertions.assertThat(result).isFalse();
    }

    @Test
    void removesAllNestedContentRecursively() {
      webDeploymentContentRepository.deploy(TEST_PROJECT, createZip(
          entry("a/b/c/deep.txt", "deep content"),
          entry("a/b/shallow.txt", "shallow")
      ));

      webDeploymentContentRepository.delete(TEST_PROJECT);

      Path projectDir = webRoot.resolve(TEST_PROJECT);
      Assertions.assertThat(Files.exists(projectDir)).isFalse();
    }
  }

  // ==================================================================================
  // exists()
  // ==================================================================================

  @Nested
  @DisplayName("exists()")
  class Exists {

    @Test
    void returnsTrueAfterDeploy() {
      webDeploymentContentRepository.deploy(TEST_PROJECT, createZip(
          entry("index.html", "<html/>")
      ));

      Assertions.assertThat(webDeploymentContentRepository.exists(TEST_PROJECT)).isTrue();
    }

    @Test
    void returnsFalseWhenNeverDeployed() {
      Assertions.assertThat(webDeploymentContentRepository.exists("neverdeployed")).isFalse();
    }

    @Test
    void returnsFalseAfterDelete() {
      webDeploymentContentRepository.deploy(TEST_PROJECT, createZip(
          entry("index.html", "<html/>")
      ));

      webDeploymentContentRepository.delete(TEST_PROJECT);

      Assertions.assertThat(webDeploymentContentRepository.exists(TEST_PROJECT)).isFalse();
    }
  }

  // ==================================================================================
  // getProjectWebPath()
  // ==================================================================================

  @Nested
  @DisplayName("getProjectWebPath()")
  class GetProjectWebPath {

    @Test
    void returnsExpectedPath() {
      Path result = webDeploymentContentRepository.getProjectWebPath(TEST_PROJECT);

      Path expected = webRoot.resolve(TEST_PROJECT);
      Assertions.assertThat(result).isEqualTo(expected);
    }

    @Test
    void returnedPathIsAbsolute() {
      Path result = webDeploymentContentRepository.getProjectWebPath(TEST_PROJECT);

      Assertions.assertThat(result).isAbsolute();
    }
  }

  // ==================================================================================
  // deleteAll()
  // ==================================================================================

  @Nested
  @DisplayName("deleteAll()")
  class DeleteAll {

    @Test
    void deletesMultipleDeploymentsAndLeavesRootIntact() {
      // Arrange: Deploy two different projects
      webDeploymentContentRepository.deploy("project-a", createZip(
          entry("index.html", "<html/>")
      ));
      webDeploymentContentRepository.deploy("project-b", createZip(
          entry("index.html", "<html/>")
      ));

      Assertions.assertThat(Files.isDirectory(webRoot.resolve("project-a"))).isTrue();
      Assertions.assertThat(Files.isDirectory(webRoot.resolve("project-b"))).isTrue();

      // Act
      webDeploymentContentRepository.deleteAll();

      // Assert: Projects are gone, but root survives
      Assertions.assertThat(Files.exists(webRoot.resolve("project-a"))).isFalse();
      Assertions.assertThat(Files.exists(webRoot.resolve("project-b"))).isFalse();
      Assertions.assertThat(Files.isDirectory(webRoot))
          .as("The webRoot directory itself must not be deleted").isTrue();
    }

    @Test
    void deletesStrayFilesInRootDirectory() throws IOException {
      // Arrange: Create a random stray file directly in the webRoot
      Path strayFile = webRoot.resolve("stray-file.txt");
      Files.writeString(strayFile, "I should not be here");
      Assertions.assertThat(Files.exists(strayFile)).isTrue();

      // Act
      webDeploymentContentRepository.deleteAll();

      // Assert: Stray file is cleaned up, but root survives
      Assertions.assertThat(Files.exists(strayFile)).isFalse();
      Assertions.assertThat(Files.isDirectory(webRoot)).isTrue();
    }

    @Test
    void completesSuccessfullyWhenDirectoryIsAlreadyEmpty() throws IOException {
      // Arrange: Ensure directory is completely empty
      try (Stream<Path> paths = Files.list(webRoot)) {
        paths
            // but not the readme file
            .filter(path -> !path.getFileName().toString().equalsIgnoreCase("readme.md"))
            .forEach(p -> {
          try {
            Files.deleteIfExists(p);
          } catch (IOException _) {
            // empty because ignored
          }
        });
      }

      // Act & Assert: Should not throw any exceptions
      org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> webDeploymentContentRepository.deleteAll());
      Assertions.assertThat(Files.isDirectory(webRoot)).isTrue();
    }

    @Test
    void preservesReadmeFileAtRootLevelButDeletesNestedReadmes() {
      // Arrange 1: Create README files at the root level (testing case-insensitivity)
      Path uppercaseReadme = webRoot.resolve("README.md");
      Path lowercaseReadme = webRoot.resolve("readme.md");

      // Arrange 2: Deploy a project that contains its own nested README.md
      webDeploymentContentRepository.deploy("project-with-readme", createZip(
          entry("README.md", "I am a nested project README and should be deleted"),
          entry("index.html", "<html/>")
      ));

      Assertions.assertThat(Files.exists(uppercaseReadme)).isTrue();
      Assertions.assertThat(Files.exists(lowercaseReadme)).isTrue();
      Assertions.assertThat(Files.exists(webRoot.resolve("project-with-readme/README.md"))).isTrue();

      // Act
      webDeploymentContentRepository.deleteAll();

      // Assert 1: Root READMEs must survive
      Assertions.assertThat(Files.exists(uppercaseReadme))
          .as("Uppercase README.md at the root should survive").isTrue();
      Assertions.assertThat(Files.exists(lowercaseReadme))
          .as("Lowercase readme.md at the root should survive").isTrue();

      // Assert 2: Project directory (and its nested README) must be deleted
      Assertions.assertThat(Files.exists(webRoot.resolve("project-with-readme")))
          .as("The project directory and all its contents should be deleted").isFalse();
    }
  }

  // ==================================================================================
  // Zip creation helpers
  // ==================================================================================

  private record ZipEntryContent(String path, String content) {}

  private static ZipEntryContent entry(String path, String content) {
    return new ZipEntryContent(path, content);
  }

  private static InputStream createZip(ZipEntryContent... entries) {
    return new ByteArrayInputStream(createZipBytes(entries));
  }

  private static byte[] createZipBytes(ZipEntryContent... entries) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
      for (ZipEntryContent e : entries) {
        zos.putNextEntry(new ZipEntry(e.path()));
        zos.write(e.content().getBytes());
        zos.closeEntry();
      }
    } catch (IOException ex) {
      throw new RuntimeException("Failed to create test zip", ex);
    }
    return baos.toByteArray();
  }

  /**
   * Creates a zip with an explicit directory entry followed by file entries.
   */
  private static InputStream createZipWithDirectoryEntry(String dirName, ZipEntryContent... files) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
      // Add explicit directory entry
      zos.putNextEntry(new ZipEntry(dirName));
      zos.closeEntry();
      // Add file entries
      for (ZipEntryContent e : files) {
        zos.putNextEntry(new ZipEntry(e.path()));
        zos.write(e.content().getBytes());
        zos.closeEntry();
      }
    } catch (IOException ex) {
      throw new RuntimeException("Failed to create test zip", ex);
    }
    return new ByteArrayInputStream(baos.toByteArray());
  }

  /**
   * Creates a valid but empty zip (no entries).
   */
  private static byte[] createEmptyZip() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    return baos.toByteArray();
  }

  /**
   * Creates a zip with a path traversal entry for security testing.
   */
  private static byte[] createMaliciousZip(String maliciousPath, String content) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
      zos.putNextEntry(new ZipEntry(maliciousPath));
      zos.write(content.getBytes());
      zos.closeEntry();
    } catch (IOException ex) {
      throw new RuntimeException("Failed to create malicious zip", ex);
    }
    return baos.toByteArray();
  }
}