package org.zim.gamsapi.Datastream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.IntegrationTest;
import java.nio.file.Files;
import java.nio.file.Path;

public class DatastreamContentRepositoryIT extends IntegrationTest {

  @Autowired
  IDatastreamContentRepository datastreamContentRepository;

  @Nested
  public class Save {

    @Test
    public void savesExpectedFileToFilesSystem() {

      final byte[] TEST_DATA = "test data".getBytes();
      final DatastreamId TEST_DATASTREAM_ID = DatastreamId.builder().digitalObject("testId").dsid("TEST").build();

      datastreamContentRepository.save(TEST_DATA, TEST_DATASTREAM_ID);

      Assertions.assertTrue(
          Files.exists(datastreamContentRepository.calcBalancedFilepath(TEST_DATASTREAM_ID))
      );

      // cleanup
      datastreamContentRepository.delete(TEST_DATASTREAM_ID);

    }

  }

  @Nested
  public class Delete {

    @Test
    public void deletesExpectedFileFromFilesSystem() {

      final byte[] TEST_DATA = "test data".getBytes();
      final DatastreamId TEST_DATASTREAM_ID = DatastreamId.builder().digitalObject("testId").dsid("TEST").build();

      datastreamContentRepository.save(TEST_DATA, TEST_DATASTREAM_ID);

      datastreamContentRepository.delete(TEST_DATASTREAM_ID);

      Assertions.assertFalse(
          Files.exists(datastreamContentRepository.calcBalancedFilepath(TEST_DATASTREAM_ID))
      );

    }

  }

  @Nested
  public class CalcBalancedFilePath {

    @Test
    public void calculatesExpectedFilePath() {

      final String EXPECTED_FILE_PATH = Path
          .of("gams","cf28f07e7385e123", "a42aa9df5e1683ba", "06f349eb9c070c80", "b3a6644bb046db81")
          .toAbsolutePath()
          .toString();

      final DatastreamId TEST_DATASTREAN_ID = DatastreamId.builder().digitalObject("testId").dsid("TEST").build();

      String result =  datastreamContentRepository
          .calcBalancedFilepath(TEST_DATASTREAN_ID).toString();

      Assertions.assertEquals(EXPECTED_FILE_PATH, result);

    }


  }

}
