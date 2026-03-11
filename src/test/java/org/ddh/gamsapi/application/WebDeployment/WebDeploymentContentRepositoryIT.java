package org.ddh.gamsapi.application.WebDeployment;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.application.WebDeployment.exceptions.WebDeploymentStorageException;
import org.ddh.gamsapi.infrastructure.System.configproperties.GAMSStorageProperties;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
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
public class WebDeploymentContentRepositoryIT extends IntegrationTest {

  private static final String TEST_PROJECT = "webtest";

  @Autowired
  private WebDeploymentContentRepository webDeploymentContentRepository;

  @Autowired
  private GAMSStorageProperties gamsStorageProperties;

  private Path webRoot;

  @BeforeEach
  public void setup() {
    webRoot = Paths.get(gamsStorageProperties.getWebRootPath()).toAbsolutePath();
  }

  @AfterEach
  @Override
  public void tearDown() {
    cleanupWebRoot();
    super.tearDown();
  }

  private void cleanupWebRoot() {
    // TODO catching those exceptions here seems weird
    try (Stream<Path> children = Files.list(webRoot)) {
      children
          .filter(p -> !p.getFileName().toString().equals("README.md"))
          .forEach(p -> {
            try (Stream<Path> walk = Files.walk(p)) {
              walk.sorted(Comparator.reverseOrder()).forEach(f -> {
                try { Files.delete(f); } catch (IOException _) {}
              });
            } catch (IOException _) {}
          });
    } catch (IOException _) {}
  }

  @Nested
  @DisplayName("deploy()")
  public class Deploy {

    @Test
    public void createsExpectedDirectoryForProject() {
      InputStream zip = createZip(
          entry("index.html", "<html/>")
      );

      webDeploymentContentRepository.deploy(TEST_PROJECT, zip);

      Path projectDir = webRoot.resolve(TEST_PROJECT);
      Assertions.assertThat(Files.isDirectory(projectDir)).isTrue();
    }

    @Test
    public void extractsSingleFileWithExpectedContent() throws IOException {
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
    public void extractsNestedDirectoryStructure() {
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
    public void returnsExpectedDeploymentStats() {
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
    public void directoryOnlyEntriesAreNotCountedAsFiles() {
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
    public void atomicSwapReplacesOldContentCompletely() throws IOException {
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
    public void noOrphanTempDirectoriesAfterSuccessfulDeploy() throws IOException {
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
        Assertions.assertThat(orphanCount).isEqualTo(0);
      }
    }

    @Test
    public void throwsOnEmptyZip() {
      byte[] emptyZip = createEmptyZip();
      InputStream zip = new ByteArrayInputStream(emptyZip);

      Assertions.assertThatThrownBy(
              () -> webDeploymentContentRepository.deploy(TEST_PROJECT, zip)
          ).isInstanceOf(WebDeploymentStorageException.class)
          .hasMessageContaining("no entries");
    }

    @Test
    public void throwsOnPathTraversalAttack() {
      byte[] maliciousZip = createMaliciousZip("../../etc/passwd", "malicious");

      Assertions.assertThatThrownBy(
              () -> webDeploymentContentRepository.deploy(
                  TEST_PROJECT, new ByteArrayInputStream(maliciousZip))
          ).isInstanceOf(WebDeploymentStorageException.class)
          .hasMessageContaining("path traversal");
    }

    @Test
    public void cleansUpTempDirectoryOnPathTraversalFailure() throws IOException {
      byte[] maliciousZip = createMaliciousZip("../../escape.txt", "data");

      try {
        webDeploymentContentRepository.deploy(
            TEST_PROJECT, new ByteArrayInputStream(maliciousZip));
      } catch (WebDeploymentStorageException expected) {
        // expected
      }

      // No temp directories should remain
      try (Stream<Path> siblings = Files.list(webRoot)) {
        long tmpCount = siblings
            .filter(p -> p.getFileName().toString().startsWith(".tmp-" + TEST_PROJECT))
            .count();
        Assertions.assertThat(tmpCount).isEqualTo(0);
      }
    }
  }

  @Nested
  @DisplayName("delete()")
  public class Delete {

    @Test
    public void returnsTrueAndRemovesDirectoryWhenDeploymentExists() {
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
    public void returnsFalseWhenNoDeploymentExists() {
      boolean result = webDeploymentContentRepository.delete("neverdeployed");

      Assertions.assertThat(result).isFalse();
    }

    @Test
    public void removesAllNestedContentRecursively() {
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
  public class Exists {

    @Test
    public void returnsTrueAfterDeploy() {
      webDeploymentContentRepository.deploy(TEST_PROJECT, createZip(
          entry("index.html", "<html/>")
      ));

      Assertions.assertThat(webDeploymentContentRepository.exists(TEST_PROJECT)).isTrue();
    }

    @Test
    public void returnsFalseWhenNeverDeployed() {
      Assertions.assertThat(webDeploymentContentRepository.exists("neverdeployed")).isFalse();
    }

    @Test
    public void returnsFalseAfterDelete() {
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
  public class GetProjectWebPath {

    @Test
    public void returnsExpectedPath() {
      Path result = webDeploymentContentRepository.getProjectWebPath(TEST_PROJECT);

      Path expected = webRoot.resolve(TEST_PROJECT);
      Assertions.assertThat(result).isEqualTo(expected);
    }

    @Test
    public void returnedPathIsAbsolute() {
      Path result = webDeploymentContentRepository.getProjectWebPath(TEST_PROJECT);

      Assertions.assertThat(result.isAbsolute()).isTrue();
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
    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
      // Intentionally empty
    } catch (IOException ex) {
      throw new RuntimeException("Failed to create empty zip", ex);
    }
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