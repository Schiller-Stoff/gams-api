package org.zim.gamsapi.Ingest.utils.Bagit;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.zim.gamsapi.Ingest.utils.Bagit.mapping.BagSipJson;
import org.zim.gamsapi.TestUtilities.TestBag;
import org.zim.gamsapi.UnitTest;

import java.io.IOException;

/**
 * TODO check BagTest
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BagDirectoryReaderTest extends UnitTest {

  Bag bag;

  @BeforeAll
  public void setup() throws IOException {
    var testBag = TestBag.loadFile();
    bag = new Bag(testBag.toPath());
  }

  @Nested
  public class ReadSIPJson {

      BagSipJson  bagSipJson;

      @BeforeEach
      public void setup(){
          bagSipJson = BagDirectoryReader.extractAndValidateSipJson(bag.getBAG_DIR_PATH());
      }

      @Test
      public void bagSipJsonIsNotNull(){
          Assertions.assertThat(bagSipJson).isNotNull();
      }

      @Test
      public void containsExpectedNumberOfContentFiles(){
          Assertions.assertThat(bagSipJson.getContentFiles().size())
                  .isEqualTo(5);
      }

      @Test
      public void createsExpectedBagSipJsonObject(){

          var bagSipJson = BagDirectoryReader.extractAndValidateSipJson(bag.getBAG_DIR_PATH());

          Assertions.assertThat(bagSipJson.getCreator())
                  .isEqualTo(TestBag.TestBagSipJson.CREATOR);

          Assertions.assertThat(bagSipJson.getId())
                  .isEqualTo(TestBag.TestBagSipJson.REC_ID);

          Assertions.assertThat(bagSipJson.getFunder())
                  .isEqualTo(TestBag.TestBagSipJson.FUNDER);

          Assertions.assertThat(bagSipJson.getProject())
                  .isEqualTo(TestBag.TestBagSipJson.PROJECT);

          Assertions.assertThat(bagSipJson.getObjectType())
                  .isEqualTo(TestBag.TestBagSipJson.OBJECT_TYPE);

          Assertions.assertThat(bagSipJson.getDescription())
                  .isEqualTo(TestBag.TestBagSipJson.DESCRIPTION);

          Assertions.assertThat(bagSipJson.getRights())
                    .isEqualTo(TestBag.TestBagSipJson.RIGHTS);

          Assertions.assertThat(bagSipJson.getPublisher())
                  .isEqualTo(TestBag.TestBagSipJson.PUBLISHER);

            Assertions.assertThat(bagSipJson.getTitle())
                    .isEqualTo(TestBag.TestBagSipJson.TITLE);

            Assertions.assertThat(bagSipJson.getMainResource())
                    .isEqualTo(TestBag.TestBagSipJson.MAIN_RESOURCE);

      }


  }

}
