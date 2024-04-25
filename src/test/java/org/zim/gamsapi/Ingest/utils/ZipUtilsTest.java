package org.zim.gamsapi.Ingest.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.zim.gamsapi.Ingest.exceptions.IngestProcessingException;
import org.zim.gamsapi.UnitTest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

      Assertions.assertThat(fileNames).contains("tei/TEI_SOURCE.xml");
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
  public class UnzipDirToTempDir {

    @Test
    public void returnsPathToTempDirWhenZippedDirIsGiven() {
      byte[] zippedDir = ZipUtils.zipDir(teiIngestDir);
      Path tempDirPath = ZipUtils.unzipToTempDir(zippedDir);
      org.junit.jupiter.api.Assertions.assertTrue(Files.exists(tempDirPath));
      org.junit.jupiter.api.Assertions.assertTrue(Files.isDirectory(tempDirPath));
    }

    @Test
    public void throwsExceptionWhenZippedDirIsInvalid() {
      byte[] invalidZippedDir = new byte[0];
      org.junit.jupiter.api.Assertions.assertThrows(IngestProcessingException.class, () -> {
        ZipUtils.unzipToTempDir(invalidZippedDir);
      });
    }

  }


}
