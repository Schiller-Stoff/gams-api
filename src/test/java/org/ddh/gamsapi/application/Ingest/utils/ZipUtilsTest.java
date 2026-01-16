package org.ddh.gamsapi.application.Ingest.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.ddh.gamsapi.application.Ingest.exceptions.IngestProcessingException;
import org.ddh.gamsapi.UnitTest;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtilsTest extends UnitTest {

  File teiIngestDir;

  private final String TESTFOLDER_LOCATION = "testfiles/tei";
  private final String TESTFILE_LOCATION = TESTFOLDER_LOCATION + "/TEI_SOURCE.xml";

  @BeforeEach
  public void setUp() throws IOException {
    teiIngestDir = new ClassPathResource(TESTFOLDER_LOCATION).getFile();
  }

  @Nested
  public class WalkZippedDir {

    @Test
    public void containsExpectedFileNames() {
      List<String> fileNames = new ArrayList<>();
      byte[] teiZip = ZipUtils.zipDir(teiIngestDir);

      ZipUtils.walkZippedDir(teiZip, (zipEntry, __) -> {
        fileNames.add(zipEntry.getName());
      });

      Assertions.assertThat(fileNames).contains("TEI_SOURCE.xml");
      Assertions.assertThat(fileNames).allMatch(fileName -> fileName.contains("."));

    }

    @Test
    public void dataHasExpectedSize() {
      byte[] teiZip = ZipUtils.zipDir(teiIngestDir);
      ZipUtils.walkZippedDir(teiZip, (zipEntry,outputStream) -> {
        // is the size from ZipEntry the same as the actual output?
        Assertions.assertThat(outputStream.size()).isEqualTo(zipEntry.getSize());
      });
    }

    @Test
    public void throwsExceptionIfNoZipGiven() {
      byte[] invalidZip = new byte[0];
      org.junit.jupiter.api.Assertions.assertThrows(IngestProcessingException.class, () -> {
        ZipUtils.walkZippedDir(invalidZip, (zipEntry, __) -> {});
      });
    }

  }

  @Nested
  public class ZipDir {

    @Test
    public void returnsNon0ByteArray() {
      byte[] teiZip = ZipUtils.zipDir(teiIngestDir);
      Assertions.assertThat(teiZip.length).isGreaterThan(0);
    }

    @Test
    public void throwsExceptionIfNoDirGiven() throws IOException {
      File teiIngestDir = new ClassPathResource(TESTFILE_LOCATION).getFile();
      org.junit.jupiter.api.Assertions.assertThrows(IngestProcessingException.class, () -> {
        ZipUtils.zipDir(teiIngestDir);
      });
    }
  }

  @Nested
  public class UnzipToTempDir {

    @Test
    public void createsValidFileFromZip(){
      byte[] zippedDir = ZipUtils.zipDir(teiIngestDir);
      Path tempDirPath = ZipUtils.unzipToTempDir(zippedDir);
      org.junit.jupiter.api.Assertions.assertTrue(Files.exists(tempDirPath));
      org.junit.jupiter.api.Assertions.assertTrue(Files.isDirectory(tempDirPath));
    }

    @Test
    public void returnsNotNullPathToTempDirWhenZippedDirIsGiven() {
      byte[] zippedDir = ZipUtils.zipDir(teiIngestDir);
      Path tempDirPath = ZipUtils.unzipToTempDir(zippedDir);
      Assertions.assertThat(tempDirPath)
          .isNotNull();
    }

    @Test
    public void throwsExceptionWhenZippedDirIsInvalid() {
      byte[] invalidZippedDir = new byte[0];
      org.junit.jupiter.api.Assertions.assertThrows(IngestProcessingException.class, () -> {
        ZipUtils.unzipToTempDir(invalidZippedDir);
      });
    }

  }

  @Nested
  public class DeleteDir {

    @Test
    public void deletesDirectoryAndAllItsContents() throws IOException, IngestProcessingException {
      Path dirPath = Files.createTempDirectory("testDir");
      Files.createTempFile(dirPath, "testFile", ".txt");

      ZipUtils.deleteDir(dirPath);

      org.junit.jupiter.api.Assertions.assertFalse(Files.exists(dirPath));
    }

    @Test
    public void deletesNestedDirectoriesAndTheirContents() throws IOException, IngestProcessingException {
      Path dirPath = Files.createTempDirectory("testDir");
      Path nestedDirPath = Files.createDirectories(dirPath.resolve("nestedDir"));
      Files.createTempFile(nestedDirPath, "testFile", ".txt");

      ZipUtils.deleteDir(dirPath);

      org.junit.jupiter.api.Assertions.assertFalse(Files.exists(dirPath));
      org.junit.jupiter.api.Assertions.assertFalse(Files.exists(nestedDirPath));
    }

    @Test
    public void throwsExceptionWhenDirectoryDoesNotExist() {
      Path dirPath = Paths.get("nonExistentDir");

      org.junit.jupiter.api.Assertions.assertThrows(IngestProcessingException.class, () -> {
        ZipUtils.deleteDir(dirPath);
      });
    }

    @Test
    public void doesNotThrowExceptionWhenDirectoryIsEmpty() throws IOException, IngestProcessingException {
      Path dirPath = Files.createTempDirectory("testDir");

      ZipUtils.deleteDir(dirPath);

      org.junit.jupiter.api.Assertions.assertFalse(Files.exists(dirPath));
    }
  }


  @Nested
  public class UnzipStreamToTempDir {

    @Test
    public void createsValidDirectoryFromZipStream() throws IOException {
      byte[] zippedDir = ZipUtils.zipDir(teiIngestDir);
      InputStream zipStream = new ByteArrayInputStream(zippedDir);

      Path tempDirPath = ZipUtils.unzipStreamToTempDir(zipStream);

      Assertions.assertThat(tempDirPath).isNotNull();
      Assertions.assertThat(Files.exists(tempDirPath)).isTrue();
      Assertions.assertThat(Files.isDirectory(tempDirPath)).isTrue();

      // Cleanup
      ZipUtils.deleteDir(tempDirPath);
    }

    @Test
    public void extractedFilesMatchOriginalStructure() throws IOException {
      byte[] zippedDir = ZipUtils.zipDir(teiIngestDir);
      InputStream zipStream = new ByteArrayInputStream(zippedDir);

      Path tempDirPath = ZipUtils.unzipStreamToTempDir(zipStream);

      // Verify expected file exists
      Path extractedFile = tempDirPath.resolve("TEI_SOURCE.xml");
      Assertions.assertThat(Files.exists(extractedFile)).isTrue();
      Assertions.assertThat(Files.isRegularFile(extractedFile)).isTrue();

      // Cleanup
      ZipUtils.deleteDir(tempDirPath);
    }

    @Test
    public void createdDirectoryHasCorrectPrefix() throws IOException {
      byte[] zippedDir = ZipUtils.zipDir(teiIngestDir);
      InputStream zipStream = new ByteArrayInputStream(zippedDir);

      Path tempDirPath = ZipUtils.unzipStreamToTempDir(zipStream);

      String dirName = tempDirPath.getFileName().toString();
      Assertions.assertThat(dirName).startsWith("gams-ingest-");

      // Cleanup
      ZipUtils.deleteDir(tempDirPath);
    }

    @Test
    public void throwsExceptionForEmptyZipStream() {
      InputStream emptyStream = new ByteArrayInputStream(new byte[0]);

      org.junit.jupiter.api.Assertions.assertThrows(
          IngestProcessingException.class,
          () -> ZipUtils.unzipStreamToTempDir(emptyStream)
      );
    }

    @Test
    public void throwsExceptionForInvalidZipData() {
      byte[] invalidData = "This is not a zip file".getBytes();
      InputStream invalidStream = new ByteArrayInputStream(invalidData);

      org.junit.jupiter.api.Assertions.assertThrows(
          IngestProcessingException.class,
          () -> ZipUtils.unzipStreamToTempDir(invalidStream)
      );
    }

    @Test
    public void throwsExceptionForZipWithNoEntries() throws IOException {
      // Create a valid but empty zip
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ZipOutputStream zos = new ZipOutputStream(baos)) {
        // Don't add any entries
      }

      InputStream emptyZipStream = new ByteArrayInputStream(baos.toByteArray());

      org.junit.jupiter.api.Assertions.assertThrows(
          IngestProcessingException.class,
          () -> ZipUtils.unzipStreamToTempDir(emptyZipStream)
      );
    }

    @Test
    public void preventsPathTraversalAttack() throws IOException {
      // Create a malicious zip with path traversal entry
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ZipOutputStream zos = new ZipOutputStream(baos)) {
        // Try to escape temp directory
        ZipEntry maliciousEntry = new ZipEntry("../../../etc/passwd");
        zos.putNextEntry(maliciousEntry);
        zos.write("malicious content".getBytes());
        zos.closeEntry();
      }

      InputStream maliciousStream = new ByteArrayInputStream(baos.toByteArray());

      IngestProcessingException exception = org.junit.jupiter.api.Assertions.assertThrows(
          IngestProcessingException.class,
          () -> ZipUtils.unzipStreamToTempDir(maliciousStream)
      );

      Assertions.assertThat(exception.getMessage())
          .contains("path traversal detected");
    }

    @Test
    public void handlesNestedDirectoriesCorrectly() throws IOException {
      // Create zip with nested structure
      Path tempSourceDir = Files.createTempDirectory("test-source");
      Path nestedDir = Files.createDirectories(tempSourceDir.resolve("level1/level2"));
      Files.createFile(nestedDir.resolve("nested-file.txt"));

      byte[] zippedDir = ZipUtils.zipDir(tempSourceDir.toFile());
      InputStream zipStream = new ByteArrayInputStream(zippedDir);

      Path extractedDir = ZipUtils.unzipStreamToTempDir(zipStream);

      Path extractedNested = extractedDir.resolve("level1/level2/nested-file.txt");
      Assertions.assertThat(Files.exists(extractedNested)).isTrue();

      // Cleanup
      ZipUtils.deleteDir(extractedDir);
      ZipUtils.deleteDir(tempSourceDir);
    }

    @Test
    public void handlesZipWithDirectoryEntriesOnly() throws IOException {
      // Create zip with only directory entries
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ZipOutputStream zos = new ZipOutputStream(baos)) {
        ZipEntry dirEntry = new ZipEntry("emptyDir/");
        zos.putNextEntry(dirEntry);
        zos.closeEntry();
      }

      InputStream zipStream = new ByteArrayInputStream(baos.toByteArray());

      Path extractedDir = ZipUtils.unzipStreamToTempDir(zipStream);

      Path emptyDir = extractedDir.resolve("emptyDir");
      Assertions.assertThat(Files.exists(emptyDir)).isTrue();
      Assertions.assertThat(Files.isDirectory(emptyDir)).isTrue();

      // Cleanup
      ZipUtils.deleteDir(extractedDir);
    }

    @Test
    public void returnsNonNullPathForValidZip() throws IOException {
      byte[] zippedDir = ZipUtils.zipDir(teiIngestDir);
      InputStream zipStream = new ByteArrayInputStream(zippedDir);

      Path result = ZipUtils.unzipStreamToTempDir(zipStream);

      Assertions.assertThat(result).isNotNull();

      // Cleanup
      ZipUtils.deleteDir(result);
    }
  }


}
