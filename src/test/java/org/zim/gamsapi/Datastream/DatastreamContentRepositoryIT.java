package org.zim.gamsapi.Datastream;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.System.configproperties.GAMSStorageProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatastreamContentRepositoryIT extends IntegrationTest {

  @Autowired
  IDatastreamContentRepository datastreamContentRepository;

  @Autowired
  GAMSStorageProperties gamsStorageProperties;

  @AfterAll
  public void tearDown(){
    datastreamContentRepository.deleteAll();
  }

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
  public class Exists {

    @Test
    public void returnsTrueIfFileExists() {

      final byte[] TEST_DATA = "test data".getBytes();
      final DatastreamId TEST_DATASTREAM_ID = DatastreamId.builder().digitalObject("testId").dsid("TEST").build();

      datastreamContentRepository.save(TEST_DATA, TEST_DATASTREAM_ID);

      Assertions.assertTrue(
          datastreamContentRepository.exists(TEST_DATASTREAM_ID)
      );

      // cleanup
      datastreamContentRepository.delete(TEST_DATASTREAM_ID);

    }

    @Test
    public void returnsFalseIfFileDoesNotExist() {

      final DatastreamId TEST_DATASTREAM_ID = DatastreamId.builder().digitalObject("testId").dsid("TEST").build();

      Assertions.assertFalse(
          datastreamContentRepository.exists(TEST_DATASTREAM_ID)
      );

    }

  }


  @Nested
  public class FindById {

    @Test
    public void returnsExpectedFileContent() throws IOException {

      final byte[] TEST_DATA = "test data".getBytes();
      final DatastreamId TEST_DATASTREAM_ID = DatastreamId.builder().digitalObject("testId").dsid("TEST").build();

      datastreamContentRepository.save(TEST_DATA, TEST_DATASTREAM_ID);

      org.assertj.core.api.Assertions.assertThat(datastreamContentRepository.findById(TEST_DATASTREAM_ID).getContentAsString(StandardCharsets.UTF_8))
              .contains("test data");

      // cleanup
      datastreamContentRepository.delete(TEST_DATASTREAM_ID);

    }



  }


  @Nested
  public class CalcBalancedFilePath {

    @Test
    public void calculatesExpectedFilePath() {

      final String EXPECTED_FILE_PATH = Path
          .of(gamsStorageProperties.getRootPath(),"cf28f07e7385e123", "a42aa9df5e1683ba", "06f349eb9c070c80", "b3a6644bb046db81")
          .toAbsolutePath()
          .toString();

      final DatastreamId TEST_DATASTREAN_ID = DatastreamId.builder().digitalObject("testId").dsid("TEST").build();

      String result =  datastreamContentRepository
          .calcBalancedFilepath(TEST_DATASTREAN_ID).toString();

      Assertions.assertEquals(EXPECTED_FILE_PATH, result);

    }


  }


  @Nested
  public class DeleteAll {

    @Test
    public void deletesExpectedFiles() {

      final byte[] TEST_DATA = "test data".getBytes();
      final DatastreamId TEST_DATASTREAM_ID = DatastreamId.builder().digitalObject("testId").dsid("TEST").build();
      final DatastreamId  TEST_DATASTREAM_ID2 = DatastreamId.builder().digitalObject("testId2").dsid("TEST").build();

      datastreamContentRepository.save(TEST_DATA, TEST_DATASTREAM_ID);
      datastreamContentRepository.save(TEST_DATA, TEST_DATASTREAM_ID2);


      datastreamContentRepository.deleteAll();

      Assertions.assertFalse(
          Files.exists(datastreamContentRepository.calcBalancedFilepath(TEST_DATASTREAM_ID))
      );

      Assertions.assertFalse(
          Files.exists(datastreamContentRepository.calcBalancedFilepath(TEST_DATASTREAM_ID2))
      );

    }

  }


}
