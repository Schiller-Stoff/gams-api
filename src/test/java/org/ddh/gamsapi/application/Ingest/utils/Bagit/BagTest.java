package org.ddh.gamsapi.application.Ingest.utils.Bagit;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.application.Ingest.exceptions.IngestProcessingException;
import org.junit.jupiter.api.*;
import org.ddh.gamsapi.domain.Datastream.utils.GAMSDsid;
import org.ddh.gamsapi.TestUtilities.TestBag;
import org.ddh.gamsapi.UnitTest;
import java.io.IOException;
import java.util.NoSuchElementException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BagTest extends UnitTest {

  Bag bag;

  @BeforeAll
  public void setup() throws IOException {
    var testBag = TestBag.loadFile();
    bag = new Bag(testBag.toPath());
  }

  @Nested
  public class FindContentFileByDsid {
    @Test
    public void findContentFileByDsidReturnsExpectedDsid() {
      var dsid = GAMSDsid.DC.getValue();
      var bagFile = bag.findContentFileByDsid(dsid);
      Assertions.assertThat(bagFile).isNotNull();
      Assertions.assertThat(bagFile.getDsid()).isEqualTo(dsid);
    }

    @Test
    public void findContentFileByDsidThrowsExceptionIfNotFound() {
      var dsid = "non-existing-dsid";
      Assertions.assertThatThrownBy(() -> bag.findContentFileByDsid(dsid)).isInstanceOf(
          IngestProcessingException.class
      );
    }
  }

  @Nested
  public class ReadBag {

    @Nested
    public class BagInfo {

      @Test
      public void bagInfoHasNoNullValues() {
        Assertions.assertThat(bag.getBagInfo()).hasNoNullFieldsOrProperties();
      }

      @Test
      public void bagInfoHasExpectedPayloadOxum() {
        Assertions.assertThat(bag.getBagInfo().getPayloadOxum()).isNotNull();
        Assertions.assertThat(bag.getBagInfo().getPayloadOxum()).isEqualTo(TestBag.TestBagInfo.PAYLOAD_OXUM);
      }

      @Test
      public void bagInfoHasExpectedContactEmail() {
        Assertions.assertThat(bag.getBagInfo().getContactMail()).isNotNull();
        Assertions.assertThat(bag.getBagInfo().getContactMail()).isEqualTo(TestBag.TestBagInfo.CONTACT_EMAIL);
      }

      @Test
      public void bagInfoHasExpectedBaggingDate() {
        Assertions.assertThat(bag.getBagInfo().getDate()).isNotNull();
        Assertions.assertThat(bag.getBagInfo().getDate()).isEqualTo(TestBag.TestBagInfo.BAGGING_DATE);
      }

      @Test
      public void bagInfoHasExpectedExternalDescription() {
        Assertions.assertThat(bag.getBagInfo().getExternalDescription()).isNotNull();
        Assertions.assertThat(bag.getBagInfo().getExternalDescription()).isEqualTo(TestBag.TestBagInfo.EXTERNAL_DESCRIPTION);
      }

    }

    @Nested
    public class BagData {
      @Test
      public void bagSipJsonContainsNoNullValues() {
        Assertions.assertThat(bag.getBagData()).hasNoNullFieldsOrProperties();
      }

      @Test
      public void bagitSipJsonIsNotNull() {
        Assertions.assertThat(bag.getBagData()).isNotNull();
      }

      @Test
      public void bagitSipJsonHasExpectedNumberOfContentFiles() {
        Assertions.assertThat(bag.getBagData().getContentFiles()).isNotNull();
        Assertions.assertThat(bag.getBagData().getContentFiles().size()).isEqualTo(7);
      }

      @Test
      public void bagitSipJsonHasExpectedId() {
        Assertions.assertThat(bag.getBagData().getId()).isNotNull();
        Assertions.assertThat(bag.getBagData().getId()).isEqualTo(TestBag.TestBagSipJson.REC_ID);
      }

      @Test
      public void bagitSipJsonHasExpectedTitle() {
        Assertions.assertThat(bag.getBagData().getTitle()).isNotNull();
        Assertions.assertThat(bag.getBagData().getTitle()).isEqualTo(TestBag.TestBagSipJson.TITLE);
      }

      @Test
      public void bagitSipJsonHasExpectedDescription() {
        Assertions.assertThat(bag.getBagData().getDescription()).isNotNull();
        Assertions.assertThat(bag.getBagData().getDescription()).isEqualTo(TestBag.TestBagSipJson.DESCRIPTION);
      }

      @Test
      public void bagitSipJsonHasExpectedCreator() {
        Assertions.assertThat(bag.getBagData().getCreator()).isNotNull();
        Assertions.assertThat(bag.getBagData().getCreator()).isEqualTo(TestBag.TestBagSipJson.CREATOR);
      }

      @Test
      public void bagitSipJsonHasExpectedRights() {
        Assertions.assertThat(bag.getBagData().getRights()).isNotNull();
        Assertions.assertThat(bag.getBagData().getRights()).isEqualTo(TestBag.TestBagSipJson.RIGHTS);
      }

      @Test
      public void bagitSipJsonHasExpectedPublisher() {
        Assertions.assertThat(bag.getBagData().getPublisher()).isNotNull();
        Assertions.assertThat(bag.getBagData().getPublisher()).isEqualTo(TestBag.TestBagSipJson.PUBLISHER);
      }

      @Test
      public void bagitSipJsonHasExpectedMainResource() {
        Assertions.assertThat(bag.getBagData().getMainResource()).isNotNull();
        Assertions.assertThat(bag.getBagData().getMainResource()).isEqualTo(TestBag.TestBagSipJson.MAIN_RESOURCE);
      }

      @Test
      public void bagitSipJsonHasExpectedCreatedBy() {
        Assertions.assertThat(bag.getBagData().getCreatedBy()).isNotNull();
        Assertions.assertThat(bag.getBagData().getCreatedBy()).isEqualTo(TestBag.TestBagSipJson.CREATED_BY);
      }

      @Test
      public void bagitSipJsonHasExpectedSchema() {
        Assertions.assertThat(bag.getBagData().getSchema()).isNotNull();
        Assertions.assertThat(bag.getBagData().getSchema()).isEqualTo(TestBag.TestBagSipJson.SCHEMA);
      }

      @Test
      public void bagitSipJsonHasExpectedSource() {
        Assertions.assertThat(bag.getBagData().getSource()).isNotNull();
        Assertions.assertThat(bag.getBagData().getSource()).isEqualTo(TestBag.TestBagSipJson.SOURCE);
      }

      @Test
      public void bagitSipJsonHasExpectedProject() {
        Assertions.assertThat(bag.getBagData().getProject()).isNotNull();
        Assertions.assertThat(bag.getBagData().getProject()).isEqualTo(TestBag.TestBagSipJson.PROJECT);
      }

      @Test
      public void bagitSipJsonHasExpectedObjectType() {
        Assertions.assertThat(bag.getBagData().getObjectType()).isNotNull();
        Assertions.assertThat(bag.getBagData().getObjectType()).isEqualTo(TestBag.TestBagSipJson.OBJECT_TYPE);
      }

      @Test
      public void bagitSipJsonHasExpectedFunder() {
        Assertions.assertThat(bag.getBagData().getFunder()).isNotNull();
        Assertions.assertThat(bag.getBagData().getFunder()).isEqualTo(TestBag.TestBagSipJson.FUNDER);
      }

    }

    @Nested
    public class BagFiles {

      @Test
      public void bagFilesAreNotNull() {
        Assertions.assertThat(bag.getContentFiles()).isNotNull();
      }

      @Test
      public void bagFilesAreNotEmpty() {
        Assertions.assertThat(bag.getContentFiles()).isNotEmpty();
      }

      @Test
      public void bagFilesHaveExpectedSize() {
        Assertions.assertThat(bag.getContentFiles().size()).isEqualTo(7);
      }

      @Test
      public void thereAreSameCountOfBagFilesAsContentFilesInSipJson() {
        final var CONTENT_FILES_COUNT = bag.getBagData().getContentFiles().size();
        final var BAG_FILES_COUNT = bag.getContentFiles().size();
        Assertions.assertThat(BAG_FILES_COUNT).isEqualTo(CONTENT_FILES_COUNT);
      }

      @Test
      public void bagFilesHaveExactlySameDsidsAsContentFilesInSipJson() {
        var expectedDsids = bag.getBagData().getContentFiles().stream().map(cf -> cf.getDsid()).toList();
        var actualDsids = bag.getContentFiles().stream().map(BagFile::getDsid).toList();
        Assertions.assertThat(actualDsids).containsExactlyInAnyOrderElementsOf(expectedDsids);
      }

      @Test
      public void bagFilesHaveNonNullAndNonEmptyProperties() {
        for(var bagFile : bag.getContentFiles()) {
          Assertions.assertThat(bagFile.getBagpath()).isNotNull().isNotEmpty();
          Assertions.assertThat(bagFile.getDsid()).isNotNull().isNotEmpty();
          Assertions.assertThat(bagFile.getMimetype()).isNotNull().isNotEmpty();
          Assertions.assertThat(bagFile.getTitle()).isNotNull().isNotEmpty();
          Assertions.assertThat(bagFile.getDescription()).isNotNull().isNotEmpty();
          Assertions.assertThat(bagFile.getCreator()).isNotNull().isNotEmpty();
          Assertions.assertThat(bagFile.getRights()).isNotNull().isNotEmpty();
          Assertions.assertThat(bagFile.getSize()).isGreaterThan(0);
          Assertions.assertThat(bagFile.getTags()).isNotNull().isNotEmpty();
          Assertions.assertThat(bagFile.getLang()).isNotNull().isNotEmpty();
          Assertions.assertThat(bagFile.getMd5Checksum()).isNotNull().isNotEmpty();
          Assertions.assertThat(bagFile.getSha512Checksum()).isNotNull().isNotEmpty();
        }
      }

      @Test
      public void contentFilesHaveChecksumsOfExpectedLength(){
        for(var bagFile : bag.getContentFiles()) {
          Assertions.assertThat(bagFile.getMd5Checksum()).isNotNull().isNotEmpty().hasSize(32);
          Assertions.assertThat(bagFile.getSha512Checksum()).isNotNull().isNotEmpty().hasSize(128);
        }
      }

      @Test
      public void bagFilesAndSipJsonContentFilesHaveExpectedSameProperties(){
        var sipJson = BagDirectoryReader.readSipJson(bag.getBAG_DIR_PATH());
        for (var sipJsonContentFile : sipJson.getContentFiles()) {

          var currentBagFile = bag.findContentFileByDsid(sipJsonContentFile.getDsid());

          var expectedDsid = sipJsonContentFile.getDsid();
          var actualDsid = currentBagFile.getDsid();
          Assertions.assertThat(actualDsid).isEqualTo(expectedDsid);

          var expectedTitle = sipJsonContentFile.getTitle();
          var actualTitle = currentBagFile.getTitle();
          Assertions.assertThat(actualTitle).isEqualTo(expectedTitle);

          var expectedDescription = sipJsonContentFile.getDescription();
          var actualDescription = currentBagFile.getDescription();
          Assertions.assertThat(actualDescription).isEqualTo(expectedDescription);

          var expectedCreator = sipJsonContentFile.getCreator();
          var actualCreator = currentBagFile.getCreator();
          Assertions.assertThat(actualCreator).isEqualTo(expectedCreator);

          var expectedRights = sipJsonContentFile.getRights();
          var actualRights = currentBagFile.getRights();
          Assertions.assertThat(actualRights).isEqualTo(expectedRights);

          var expectedSize = sipJsonContentFile.getSize();
          var actualSize = currentBagFile.getSize();
          Assertions.assertThat(actualSize).isEqualTo(expectedSize);

          var expectedMimetype = sipJsonContentFile.getMimetype();
          var actualMimetype = currentBagFile.getMimetype();
          Assertions.assertThat(actualMimetype).isEqualTo(expectedMimetype);

          var expectedBagpath = sipJsonContentFile.getBagpath();
          var actualBagpath = currentBagFile.getBagpath();
          Assertions.assertThat(actualBagpath).isEqualTo(expectedBagpath);

          var expectedTags = sipJsonContentFile.getTags();
          var actualTags = currentBagFile.getTags();
          Assertions.assertThat(actualTags).containsExactlyInAnyOrderElementsOf(expectedTags);

          var expectedLang = sipJsonContentFile.getLang();
          var actualLang = currentBagFile.getLang();
          Assertions.assertThat(actualLang).containsExactlyInAnyOrderElementsOf(expectedLang);

        }
      }

    }

    @Nested
    public class BagMeta {
      @Test
      public void bagMetaIsNotNull() {
        Assertions.assertThat(bag.getBagMeta()).isNotNull();
      }

      @Test
      public void bagMetaHasNoNullValues() {
        Assertions.assertThat(bag.getBagMeta()).hasNoNullFieldsOrProperties();
      }

      @Test
      public void bagMetaHasExpectedBagitVersion() {
        Assertions.assertThat(bag.getBagMeta().getBagItVersion()).isNotNull();
        Assertions.assertThat(bag.getBagMeta().getBagItVersion()).isEqualTo(TestBag.BagitTxt.BAGIT_VERSION);
      }

      @Test
      public void bagMetaHasExpectedTagFileCharacterEncoding() {
        Assertions.assertThat(bag.getBagMeta().getTagFileCharacterEncoding()).isNotNull();
        Assertions.assertThat(bag.getBagMeta().getTagFileCharacterEncoding()).isEqualTo(TestBag.BagitTxt.TAG_FILE_CHARACTER_ENCODING);
      }

    }
  }

}
