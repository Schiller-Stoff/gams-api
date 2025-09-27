package org.zim.gamsapi.Ingest.utils.Bagit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.zim.gamsapi.Datastream.GAMSDsid;
import org.zim.gamsapi.TestUtilities.TestBag;
import org.zim.gamsapi.UnitTest;
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
    public void findContentFileByDsidThrowsNoSuchElementExceptionIfNotFound() {
      var dsid = "non-existing-dsid";
      Assertions.assertThatThrownBy(() -> bag.findContentFileByDsid(dsid)).isInstanceOf(
          NoSuchElementException.class
      );
    }
  }

  @Nested
  public class ReadBag {

    @Nested
    public class BagInfo {
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
      public void bagInfoHasExpectedBaggingTime() {
        Assertions.assertThat(bag.getBagInfo().getTime()).isNotNull();
        Assertions.assertThat(bag.getBagInfo().getTime()).isEqualTo(TestBag.TestBagInfo.BAGGING_TIME);
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
      public void bagitSipJsonIsNotNull() {
        Assertions.assertThat(bag.getBagData()).isNotNull();
      }

      @Test
      public void bagitSipJsonHasExpectedNumberOfContentFiles() {
        Assertions.assertThat(bag.getBagData().getContentFiles()).isNotNull();
        Assertions.assertThat(bag.getBagData().getContentFiles().size()).isEqualTo(5);
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
      public void bagitSipJsonHasExepectedCreator() {
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

      // TODO add createdBy test!
      @Test
      @Disabled("Property missing on sip json java class")
      public void bagitSipJsonHasExpectedCreatedBy() {
        //Assertions.assertThat(bag.getBagitSipJson().getCreatedBy()).isNotNull();
        //Assertions.assertThat(bag.getBagitSipJson().getCreatedBy()).isEqualTo(TestBag.TestBagSipJson.CREATED_BY);
      }

      // TODO add schema test!
      @Test
      @Disabled("Property missing on sip json java class")
      public void bagitSipJsonHasExpectedSchema() {
        //Assertions.assertThat(bag.getBagitSipJson().getSchema()).isNotNull();
        //Assertions.assertThat(bag.getBagitSipJson().getSchema()).isEqualTo(TestBag.TestBagSipJson.SCHEMA);
      }

      @Test
      @Disabled("Property missing on sip json java class")
      public void bagitSipJsonHasExpectedSource() {
        //Assertions.assertThat(bag.getBagitSipJson().getSource()).isNotNull();
        //Assertions.assertThat(bag.getBagitSipJson().getSource()).isEqualTo(TestBag.TestBagSipJson.SOURCE);
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
        Assertions.assertThat(bag.getContentFiles().size()).isEqualTo(5);
      }

      @Test
      public void thereAreSameCountOfBagFilesAsContentFilesInSipJson() {
        final var CONTENT_FILES_COUNT = bag.getBagData().getContentFiles().size();
        final var BAG_FILES_COUNT = bag.getContentFiles().size();;
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
        var sipJson = BagDirectoryReader.extractAndValidateSipJson(bag.getBAG_DIR_PATH());
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
  }

}
