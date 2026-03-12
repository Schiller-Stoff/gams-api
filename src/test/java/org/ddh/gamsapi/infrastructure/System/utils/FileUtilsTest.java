package org.ddh.gamsapi.infrastructure.System.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ddh.gamsapi.UnitTest;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileUtilsTest extends UnitTest {


  @Nested
  class SplitStringByN {

    @Test
    void splitStringByN_SplitsCorrectly() {
      List<String> result = FileUtils.splitStringByN("abcdefghij", 2);
      assertEquals(List.of("ab", "cd", "ef", "gh", "ij"), result);
    }

    @Test
    void splitStringByN_EmptyString() {
      List<String> result = FileUtils.splitStringByN("", 2);
      assertTrue(result.isEmpty());
    }

    @Test
    void splitStringByN_SingleCharacter() {
      List<String> result = FileUtils.splitStringByN("a", 2);
      assertEquals(List.of("a"), result);
    }

    @Test
    void splitStringByN_NGreaterThanStringLength() {
      List<String> result = FileUtils.splitStringByN("abc", 5);
      assertEquals(List.of("abc"), result);
    }

  }


  @Nested
  class BalanceFileNameToFolderHierarchy {
    @Test
    void balanceFilenameToFolderHierarchy_BalancesCorrectly() {
      String result = FileUtils.balanceFilenameToFolderHierarchy("filename.txt", 2);
      assertEquals("fi" + File.separator + "le" + File.separator + "na" + File.separator + "me.txt", result);
    }

    @Test
    void balanceFilenameToFolderHierarchy_EmptyFilename() {
      String result = FileUtils.balanceFilenameToFolderHierarchy("", 2);
      assertEquals("", result);
    }

    @Test
    void balanceFilenameToFolderHierarchy_SingleCharacterFilename() {
      String result = FileUtils.balanceFilenameToFolderHierarchy("a", 2);
      assertEquals("a", result);
    }

    @Test
    void balanceFilenameToFolderHierarchy_NGreaterThanFilenameLength() {
      String result = FileUtils.balanceFilenameToFolderHierarchy("abc", 5);
      assertEquals("abc", result);
    }
  }


  @Nested
  class CalcSha256Hex {

    @Test
    void generatesExpectedValue() throws NoSuchAlgorithmException {
      String toHash = "test";
      String expected = "36f028580bb02cc8272a9a020f4200e346e276ae664e45ee80745574e2f5ab80";
      Assertions.assertEquals(expected, FileUtils.calcSha256Hex(toHash));
    }

  }



  @Nested
  @DisplayName("emptyDirectory with skipPaths")
  class EmptyDirectoryWithSkipPathsTests {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Skipped root-level file is preserved")
    void skipsRootLevelFile() throws IOException {
      Files.createFile(tempDir.resolve("README.md"));
      Files.createFile(tempDir.resolve("delete-me.txt"));

      FileUtils.emptyDirectory(tempDir.toFile(), Set.of("README.md"));

      org.assertj.core.api.Assertions.assertThat(tempDir.resolve("README.md")).exists();
      org.assertj.core.api.Assertions.assertThat(tempDir.resolve("delete-me.txt")).doesNotExist();
    }

    @Test
    @DisplayName("Skipped nested file is preserved while siblings are deleted")
    void skipsNestedFile() throws IOException {
      Path subDir = Files.createDirectory(tempDir.resolve("subdir"));
      Files.createFile(subDir.resolve("keep.xml"));
      Files.createFile(subDir.resolve("remove.txt"));

      FileUtils.emptyDirectory(tempDir.toFile(), Set.of("subdir/keep.xml"));

      org.assertj.core.api.Assertions.assertThat(tempDir.resolve("subdir/keep.xml")).exists();
      org.assertj.core.api.Assertions.assertThat(tempDir.resolve("subdir/remove.txt")).doesNotExist();
    }

    @Test
    @DisplayName("Skipped directory preserves entire subtree")
    void skipsEntireDirectory() throws IOException {
      Path subDir = Files.createDirectory(tempDir.resolve("subdir"));
      Files.createFile(subDir.resolve("nested.txt"));
      Files.createFile(tempDir.resolve("root-file.txt"));

      FileUtils.emptyDirectory(tempDir.toFile(), Set.of("subdir"));

      org.assertj.core.api.Assertions.assertThat(tempDir.resolve("subdir/nested.txt")).exists();
      org.assertj.core.api.Assertions.assertThat(tempDir.resolve("root-file.txt")).doesNotExist();
    }

    @Test
    @DisplayName("Empty skip set behaves like no-arg version")
    void emptySkipSetDeletesEverything() throws IOException {
      Files.createFile(tempDir.resolve("file.txt"));

      FileUtils.emptyDirectory(tempDir.toFile(), Set.of());

      org.assertj.core.api.Assertions.assertThat(tempDir).isEmptyDirectory();
    }

    @Test
    @DisplayName("Parent directory of skipped file is preserved")
    void parentOfSkippedFileIsPreserved() throws IOException {
      Path subDir = Files.createDirectory(tempDir.resolve("subdir"));
      Files.createFile(subDir.resolve("keep.txt"));

      FileUtils.emptyDirectory(tempDir.toFile(), Set.of("subdir/keep.txt"));

      org.assertj.core.api.Assertions.assertThat(tempDir.resolve("subdir")).exists().isDirectory();
      org.assertj.core.api.Assertions.assertThat(tempDir.resolve("subdir/keep.txt")).exists();
    }
  }



}
