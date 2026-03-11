package org.ddh.gamsapi.application.WebDeployment;

import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.WebDeployment.exceptions.WebDeploymentStorageException;
import org.ddh.gamsapi.infrastructure.System.configproperties.GAMSStorageProperties;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Manages filesystem operations for static web deployments.
 * Stores extracted site content under {webRootPath}/{projectAbbr}/.
 * <p>
 * Uses atomic swap deployment: extract to temp dir, then rename
 * to prevent nginx from serving a half-deployed site.
 */
@Repository
@Slf4j
public class WebDeploymentContentRepository {

  private final Path webRoot;

  public WebDeploymentContentRepository(GAMSStorageProperties gamsStorageProperties) {
    this.webRoot = Paths.get(gamsStorageProperties.getWebRootPath()).toAbsolutePath();

    if (!Files.exists(webRoot)) {
      try {
        Files.createDirectories(webRoot);
        log.info("Created web deployment root directory at {}", webRoot);
      } catch (IOException e) {
        throw new WebDeploymentStorageException(
            "Could not create web deployment root at " + webRoot
                + ". Original error: " + e.getMessage(), e);
      }
    }
  }

  /**
   * Deploys a static site from a zip input stream for the given project.
   * Uses atomic swap: extract → rename old → rename new → delete old.
   *
   * @param projectAbbr the project abbreviation (determines target directory)
   * @param zipStream   input stream of the zip archive
   * @return deployment statistics (file count, total size in bytes)
   */
  public DeploymentStats deploy(String projectAbbr, InputStream zipStream) {

    Path targetDir = webRoot.resolve(projectAbbr);
    Path tempDir = webRoot.resolve(".tmp-" + projectAbbr + "-" + UUID.randomUUID());
    Path oldDir = webRoot.resolve(".old-" + projectAbbr + "-" + UUID.randomUUID());

    try {
      // Phase 1: Extract zip to temp directory
      DeploymentStats stats = extractZip(zipStream, tempDir);

      // Phase 2: Atomic swap
      if (Files.exists(targetDir)) {
        Files.move(targetDir, oldDir, StandardCopyOption.ATOMIC_MOVE);
      }

      Files.move(tempDir, targetDir, StandardCopyOption.ATOMIC_MOVE);
      log.info("Successfully deployed web content for project {} ({} files, {} bytes)",
          projectAbbr, stats.fileCount(), stats.totalSize());

      // Phase 3: Cleanup old deployment (best effort)
      if (Files.exists(oldDir)) {
        deleteDirectoryQuietly(oldDir);
      }

      return stats;

    } catch (Exception e) {
      // Cleanup temp dir on ANY failure (IOException + RuntimeException like
      // WebDeploymentStorageException from path traversal detection)
      if (Files.exists(tempDir)) {
        deleteDirectoryQuietly(tempDir);
      }
      if (e instanceof WebDeploymentStorageException wdse) {
        throw wdse;
      }
      throw new WebDeploymentStorageException(
          "Failed to deploy web content for project " + projectAbbr
              + ". Original error: " + e.getMessage(), e);
    }
  }

  /**
   * Deletes the deployed static site for a project.
   *
   * @param projectAbbr the project abbreviation
   * @return true if a deployment existed and was deleted, false if nothing existed
   */
  public boolean delete(String projectAbbr) {
    Path targetDir = webRoot.resolve(projectAbbr);
    if (!Files.exists(targetDir)) {
      return false;
    }

    try {
      deleteDirectory(targetDir);
      log.info("Deleted web deployment for project {}", projectAbbr);
      return true;
    } catch (IOException e) {
      throw new WebDeploymentStorageException(
          "Failed to delete web deployment for project " + projectAbbr
              + ". Original error: " + e.getMessage(), e);
    }
  }

  /**
   * Checks whether a deployment exists on the filesystem.
   */
  public boolean exists(String projectAbbr) {
    return Files.isDirectory(webRoot.resolve(projectAbbr));
  }

  /**
   * Returns the resolved path for a project's web deployment directory.
   */
  public Path getProjectWebPath(String projectAbbr) {
    return webRoot.resolve(projectAbbr);
  }


  /**
   * Extracts a zip stream to the given target directory.
   * Validates against path traversal and symlinks.
   */
  private DeploymentStats extractZip(InputStream zipStream, Path targetDir) throws IOException {

    Files.createDirectories(targetDir);
    Path normalizedTarget = targetDir.normalize();

    AtomicInteger fileCount = new AtomicInteger(0);
    AtomicLong totalSize = new AtomicLong(0);

    try (ZipInputStream zis = new ZipInputStream(zipStream)) {
      ZipEntry entry = zis.getNextEntry();

      if (entry == null) {
        throw new WebDeploymentStorageException(
            "Invalid zip file: no entries found");
      }

      while (entry != null) {
        Path entryPath = targetDir.resolve(entry.getName());

        // Security: prevent path traversal attacks
        if (!entryPath.normalize().startsWith(normalizedTarget)) {
          throw new WebDeploymentStorageException(
              "Invalid zip entry: " + entry.getName() + " (path traversal detected)");
        }

        if (entry.isDirectory()) {
          Files.createDirectories(entryPath);
        } else {
          // Ensure parent directories exist
          Files.createDirectories(entryPath.getParent());
          long bytesWritten = Files.copy(zis, entryPath);
          fileCount.incrementAndGet();
          totalSize.addAndGet(bytesWritten);
          log.trace("Extracted: {}", entry.getName());
        }

        zis.closeEntry();
        entry = zis.getNextEntry();
      }
    }

    return new DeploymentStats(fileCount.get(), totalSize.get());
  }

  private void deleteDirectory(Path dir) throws IOException {
    try (Stream<Path> paths = Files.walk(dir)) {
      paths.sorted(Comparator.reverseOrder())
          .forEach(path -> {
            try {
              Files.delete(path);
            } catch (IOException e) {
              throw new WebDeploymentStorageException(
                  "Failed to delete path during cleanup: " + path
                      + ". Original error: " + e.getMessage(), e);
            }
          });
    }
  }

  private void deleteDirectoryQuietly(Path dir) {
    try {
      deleteDirectory(dir);
    } catch (Exception e) {
      log.warn("Failed to cleanup directory {}: {}", dir, e.getMessage());
    }
  }

  /**
   * Deployment statistics returned after extracting a zip.
   */
  public record DeploymentStats(int fileCount, long totalSize) {}
}