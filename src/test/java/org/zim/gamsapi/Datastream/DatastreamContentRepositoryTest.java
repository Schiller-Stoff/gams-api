package org.zim.gamsapi.Datastream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.IntegrationTest;

import java.nio.file.Path;

public class DatastreamContentRepositoryTest extends IntegrationTest {

  @Autowired
  IDatastreamContentRepository datastreamContentRepository;

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
