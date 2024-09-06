package org.zim.gamsapi.System.utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.UnitTest;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileUtilsTest extends UnitTest {


  @Nested
  public  class SplitStringByN {

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
  public class BalanceFileNameToFolderHierarchy {
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





}
