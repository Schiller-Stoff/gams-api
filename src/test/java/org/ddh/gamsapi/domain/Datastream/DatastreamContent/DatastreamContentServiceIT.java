package org.ddh.gamsapi.domain.Datastream.DatastreamContent;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.ddh.gamsapi.domain.Datastream.DatastreamId;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentService;
import org.ddh.gamsapi.IntegrationTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class DatastreamContentServiceIT extends IntegrationTest {

  @Autowired
  IDatastreamContentRepository datastreamContentRepository;

  @Autowired
  IDatastreamContentService datastreamContentService;


  @Nested
  public class LoadFile {

    @Test
    public void loadedFileContainsExpectedString() throws IOException {
      final byte[] TEST_DATA = "test data".getBytes();
      final DatastreamId TEST_DATASTREAM_ID = DatastreamId.builder().digitalObject("testId").dsid("TEST").build();

      datastreamContentRepository.save(TEST_DATA, TEST_DATASTREAM_ID);

      InputStreamResource inputStreamResource = datastreamContentService.load(TEST_DATASTREAM_ID);

      Assertions.assertThat(inputStreamResource).isNotNull();
      Assertions.assertThat(inputStreamResource.getContentAsString(StandardCharsets.UTF_8)).contains("test data");


    }


  }

}
