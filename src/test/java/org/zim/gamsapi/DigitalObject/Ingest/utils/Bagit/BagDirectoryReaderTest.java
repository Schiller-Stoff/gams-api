package org.zim.gamsapi.DigitalObject.Ingest.utils.Bagit;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.zim.gamsapi.DigitalObject.Ingest.exceptions.IngestProcessingException;
import org.zim.gamsapi.DigitalObject.Ingest.utils.Bagit.Bag;
import org.zim.gamsapi.DigitalObject.Ingest.utils.Bagit.BagDirectoryReader;
import org.zim.gamsapi.DigitalObject.Ingest.utils.Bagit.BagFilePaths;
import org.zim.gamsapi.DigitalObject.Ingest.utils.Bagit.mapping.BagSipJson;
import org.zim.gamsapi.TestUtilities.TestBag;
import org.zim.gamsapi.UnitTest;
import java.io.IOException;
import java.nio.file.Path;

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
          bagSipJson = BagDirectoryReader.readSipJson(bag.getBAG_DIR_PATH());
      }

      @Test
      public void notFoundSipJsonThrowsIngestPreprocessingException(){
          Assertions.assertThatThrownBy(() -> BagDirectoryReader.readSipJson(Path.of("notExist")))
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

          var bagSipJson = BagDirectoryReader.readSipJson(bag.getBAG_DIR_PATH());

          Assertions.assertThat(bagSipJson.getCreator())
                  .isEqualTo(TestBag.TestBagSipJson.CREATOR);

          Assertions.assertThat(bagSipJson.getRecid())
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

  @Nested
  public class ReadBagInfo {
      @Test
      public void createsExpectedBagInfoObject(){
          var bagInfo = BagDirectoryReader.readBagInfoFile(bag.getBAG_DIR_PATH());
          Assertions.assertThat(bagInfo).isNotNull();
          Assertions.assertThat(bagInfo.getTime()).isEqualTo(TestBag.TestBagInfo.BAGGING_TIME);
          Assertions.assertThat(bagInfo.getDate()).isEqualTo(TestBag.TestBagInfo.BAGGING_DATE);
          Assertions.assertThat(bagInfo.getContactMail()).isEqualTo(TestBag.TestBagInfo.CONTACT_EMAIL);
          Assertions.assertThat(bagInfo.getPayloadOxum()).isEqualTo(TestBag.TestBagInfo.PAYLOAD_OXUM);
          Assertions.assertThat(bagInfo.getExternalDescription()).isEqualTo(TestBag.TestBagInfo.EXTERNAL_DESCRIPTION);
      }

      @Test
      public void notFoundBagInfoThrowsIngestPreprocessingException(){
          Assertions.assertThatThrownBy(() -> BagDirectoryReader.readBagInfoFile(Path.of("notExist")))
                  .isInstanceOf(IngestProcessingException.class);
      }


  }

  @Nested
  public class ReadSha512ManifestFile {

      @Test
      public void notFoundManifestThrowsIngestPreprocessingException(){
          Assertions.assertThatThrownBy(() -> BagDirectoryReader.readSha512ManifestFile(Path.of("notExist")))
                  .isInstanceOf(IngestProcessingException.class);
      }

      @Test
      public void returnsNonNullManifestObject(){
          var sha512Manifest = BagDirectoryReader.readSha512ManifestFile(bag.getBAG_DIR_PATH());
          Assertions.assertThat(sha512Manifest).isNotNull();
      }

      @Test
      public void createsExpectedSha512ManifestObject() {
          var sha512Manifest = BagDirectoryReader.readSha512ManifestFile(bag.getBAG_DIR_PATH());
          Assertions.assertThat(sha512Manifest.size()).isEqualTo(6);
          Assertions.assertThat(sha512Manifest.get("data/content/DC.xml")).isEqualTo("16e0517a67c3b8c65b4d6fa159236a1cb005d278a9af6de829c8b69ff74c83c9d2848911db607b9a51d205b0dcd98495b4aee8dd107afa35aa1e8d8226c1b259");
      }


  }

  @Nested
  public class ReadMd5ManifestFile {

    @Test
    public void notFoundManifestThrowsIngestPreprocessingException(){
        Assertions.assertThatThrownBy(() -> BagDirectoryReader.readMd5ManifestFile(Path.of("notExist")))
                .isInstanceOf(IngestProcessingException.class);
    }

    @Test
    public void returnsNonNullManifestObject(){
        var md5Manifest = BagDirectoryReader.readMd5ManifestFile(bag.getBAG_DIR_PATH());
        Assertions.assertThat(md5Manifest).isNotNull();
    }

    @Test
    public void createsExpectedMd5ManifestObject() {
        var md5Manifest = BagDirectoryReader.readMd5ManifestFile(bag.getBAG_DIR_PATH());
        Assertions.assertThat(md5Manifest.size()).isEqualTo(6);
        Assertions.assertThat(md5Manifest.get("data/content/DC.xml")).isEqualTo("140193d9633d8449ee1bff28030fe045");
    }

  }

  @Nested
  public class ReadKeyValueTxtFile {

      @Test
      public void notFoundKeyValueTxtThrowsIngestPreprocessingException(){
          Assertions.assertThatThrownBy(() -> BagDirectoryReader.readKeyValueTxtFile("notExist"))
                  .isInstanceOf(IngestProcessingException.class);
      }

      @Test
      public void returnsNonNullKeyValueMap(){
          var keyValueMap = BagDirectoryReader.readKeyValueTxtFile(
                  bag.getBAG_DIR_PATH().resolve(BagFilePaths.BAG_INFO_FILE_PATH.name).toString()
          );
          Assertions.assertThat(keyValueMap).isNotNull();
      }

      @Test
      public void createsExpectedKeyValueMapSizeFromBagInfo() {
          var keyValueMap = BagDirectoryReader.readKeyValueTxtFile(
                  bag.getBAG_DIR_PATH().resolve(BagFilePaths.BAG_INFO_FILE_PATH.name).toString()
          );
          Assertions.assertThat(keyValueMap.size()).isEqualTo(5);
      }

      @Test
      public void createsExpectedKeyValueMapSizeFromBagitTxt() {
          var keyValueMap = BagDirectoryReader.readKeyValueTxtFile(
                  bag.getBAG_DIR_PATH().resolve(BagFilePaths.BAG_TXT_FILE_PATH.name).toString()
          );
          Assertions.assertThat(keyValueMap.size()).isEqualTo(2);
      }

  }

  @Nested
  public class ReadBagItTxt {

      @Test
      public void notFoundBagItTxtThrowsIngestPreprocessingException(){
          Assertions.assertThatThrownBy(() -> BagDirectoryReader.readBagItTxtFile(Path.of("notExist")))
                  .isInstanceOf(IngestProcessingException.class);
      }

      @Test
      public void returnsNonNullBagMetaObject(){
          var bagMeta = BagDirectoryReader.readBagItTxtFile(bag.getBAG_DIR_PATH());
          Assertions.assertThat(bagMeta).isNotNull();
      }

      @Test
      public void readInBagMetaObjectHasNoNullFields(){
          var bagMeta = BagDirectoryReader.readBagItTxtFile(bag.getBAG_DIR_PATH());
          Assertions.assertThat(bagMeta).hasNoNullFieldsOrProperties();
      }

      @Test
      public void createsExpectedBagMetaObject(){
          var bagMeta = BagDirectoryReader.readBagItTxtFile(bag.getBAG_DIR_PATH());
          Assertions.assertThat(bagMeta).isNotNull();
          Assertions.assertThat(bagMeta.getBagItVersion()).isEqualTo(TestBag.BagitTxt.BAGIT_VERSION);
          Assertions.assertThat(bagMeta.getTagFileCharacterEncoding()).isEqualTo(TestBag.BagitTxt.TAG_FILE_CHARACTER_ENCODING);
      }

  }

}
