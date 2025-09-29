package org.zim.gamsapi.Ingest.utils.Bagit;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.zim.gamsapi.Ingest.exceptions.IngestProcessingException;
import org.zim.gamsapi.Ingest.utils.Bagit.mapping.BagSipJson;
import org.zim.gamsapi.TestUtilities.TestBag;
import org.zim.gamsapi.UnitTest;

import java.io.IOException;
import java.nio.file.Path;

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
      public void notFoundSipJsonThrowsIngestPreprocessingException(){
          Assertions.assertThatThrownBy(() -> BagDirectoryReader.extractAndValidateSipJson(Path.of("notExist")))
                  .isInstanceOf(IngestProcessingException.class);
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

      @Test
      public void createsExpectedContentFiles(){
          bagSipJson.getContentFiles().forEach(bagSipJsonContentFile -> {
             Assertions.assertThat(bagSipJsonContentFile.getBagpath()).contains("data/content/");
             Assertions.assertThat(bagSipJsonContentFile.getSize()).isGreaterThan(0);
             Assertions.assertThat(bagSipJsonContentFile.getTitle().length()).isGreaterThan(3);
             Assertions.assertThat(bagSipJsonContentFile.getDescription().length()).isGreaterThan(3);
             Assertions.assertThat(bagSipJsonContentFile.getMimetype()).contains("/");
             Assertions.assertThat(bagSipJsonContentFile.getLang().size()).isEqualTo(3);
             Assertions.assertThat(bagSipJsonContentFile.getTags().size()).isEqualTo(3);
             Assertions.assertThat(bagSipJsonContentFile.getCreator().length()).isGreaterThan(1);
             Assertions.assertThat(bagSipJsonContentFile.getRights().length()).isGreaterThan(5);
          });

      }


  }

}
