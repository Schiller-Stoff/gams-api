package org.zim.gamsapi.Ingest.utils.Bagit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.zim.gamsapi.TestUtilities.TestBag;
import org.zim.gamsapi.UnitTest;
import java.io.IOException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BagTest extends UnitTest {

  Bag bag;

  @BeforeAll
  public void setup() throws IOException {
    var testBag = TestBag.loadFile();
    bag = new Bag(testBag.toPath());
  }

  @Nested
  public class ReadBag {

    @Nested
    public class BagInfo {
      @Test
      public void bagInfoHasExpectedPayloadOxum() {
        Assertions.assertThat(bag.getBagItInfo().getPayloadOxum()).isNotNull();
        Assertions.assertThat(bag.getBagItInfo().getPayloadOxum()).isEqualTo(TestBag.TestBagInfo.PAYLOAD_OXUM);
      }

      @Test
      public void bagInfoHasExpectedContactEmail() {
        Assertions.assertThat(bag.getBagItInfo().getContactMail()).isNotNull();
        Assertions.assertThat(bag.getBagItInfo().getContactMail()).isEqualTo(TestBag.TestBagInfo.CONTACT_EMAIL);
      }

      @Test
      public void bagInfoHasExpectedBaggingDate() {
        Assertions.assertThat(bag.getBagItInfo().getDate()).isNotNull();
        Assertions.assertThat(bag.getBagItInfo().getDate()).isEqualTo(TestBag.TestBagInfo.BAGGING_DATE);
      }

      @Test
      public void bagInfoHasExpectedBaggingTime() {
        Assertions.assertThat(bag.getBagItInfo().getTime()).isNotNull();
        Assertions.assertThat(bag.getBagItInfo().getTime()).isEqualTo(TestBag.TestBagInfo.BAGGING_TIME);
      }

      @Test
      public void bagInfoHasExpectedExternalDescription() {
        Assertions.assertThat(bag.getBagItInfo().getExternalDescription()).isNotNull();
        Assertions.assertThat(bag.getBagItInfo().getExternalDescription()).isEqualTo(TestBag.TestBagInfo.EXTERNAL_DESCRIPTION);
      }

    }

    @Nested
    public class BagitSipJson {
      @Test
      public void bagitSipJsonIsNotNull() {
        Assertions.assertThat(bag.getBagitSipJson()).isNotNull();
      }

      @Test
      public void bagitSipJsonHasExpectedNumberOfContentFiles() {
        Assertions.assertThat(bag.getBagitSipJson().getContentFiles()).isNotNull();
        Assertions.assertThat(bag.getBagitSipJson().getContentFiles().size()).isEqualTo(5);
      }

      @Test
      public void bagitSipJsonHasExpectedId() {
        Assertions.assertThat(bag.getBagitSipJson().getId()).isNotNull();
        Assertions.assertThat(bag.getBagitSipJson().getId()).isEqualTo(TestBag.TestBagSipJson.REC_ID);
      }

      @Test
      public void bagitSipJsonHasExpectedTitle() {
        Assertions.assertThat(bag.getBagitSipJson().getTitle()).isNotNull();
        Assertions.assertThat(bag.getBagitSipJson().getTitle()).isEqualTo(TestBag.TestBagSipJson.TITLE);
      }

      @Test
      public void bagitSipJsonHasExpectedDescription() {
        Assertions.assertThat(bag.getBagitSipJson().getDescription()).isNotNull();
        Assertions.assertThat(bag.getBagitSipJson().getDescription()).isEqualTo(TestBag.TestBagSipJson.DESCRIPTION);
      }

      @Test
      public void bagitSipJsonHasExepectedCreator() {
        Assertions.assertThat(bag.getBagitSipJson().getCreator()).isNotNull();
        Assertions.assertThat(bag.getBagitSipJson().getCreator()).isEqualTo(TestBag.TestBagSipJson.CREATOR);
      }

      @Test
      public void bagitSipJsonHasExpectedRights() {
        Assertions.assertThat(bag.getBagitSipJson().getRights()).isNotNull();
        Assertions.assertThat(bag.getBagitSipJson().getRights()).isEqualTo(TestBag.TestBagSipJson.RIGHTS);
      }

      @Test
      public void bagitSipJsonHasExpectedPublisher() {
        Assertions.assertThat(bag.getBagitSipJson().getPublisher()).isNotNull();
        Assertions.assertThat(bag.getBagitSipJson().getPublisher()).isEqualTo(TestBag.TestBagSipJson.PUBLISHER);
      }

      @Test
      public void bagitSipJsonHasExpectedMainResource() {
        Assertions.assertThat(bag.getBagitSipJson().getMainResource()).isNotNull();
        Assertions.assertThat(bag.getBagitSipJson().getMainResource()).isEqualTo(TestBag.TestBagSipJson.MAIN_RESOURCE);
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
        Assertions.assertThat(bag.getBagitSipJson().getProject()).isNotNull();
        Assertions.assertThat(bag.getBagitSipJson().getProject()).isEqualTo(TestBag.TestBagSipJson.PROJECT);
      }

      @Test
      public void bagitSipJsonHasExpectedObjectType() {
        Assertions.assertThat(bag.getBagitSipJson().getObjectType()).isNotNull();
        Assertions.assertThat(bag.getBagitSipJson().getObjectType()).isEqualTo(TestBag.TestBagSipJson.OBJECT_TYPE);
      }

      @Test
      public void bagitSipJsonHasExpectedFunder() {
        Assertions.assertThat(bag.getBagitSipJson().getFunder()).isNotNull();
        Assertions.assertThat(bag.getBagitSipJson().getFunder()).isEqualTo(TestBag.TestBagSipJson.FUNDER);
      }

    }

    @Nested
    public class BagFiles {

      @Test
      public void bagFilesAreNotNull() {
        Assertions.assertThat(bag.getBagFiles()).isNotNull();
      }

      @Test
      public void bagFilesAreNotEmpty() {
        Assertions.assertThat(bag.getBagFiles()).isNotEmpty();
      }

      @Test
      public void bagFilesHaveExpectedSize() {
        Assertions.assertThat(bag.getBagFiles().size()).isEqualTo(5);
      }

      @Test
      public void thereAreSameCountOfBagFilesAsContentFilesInSipJson() {
        final var CONTENT_FILES_COUNT = bag.getBagitSipJson().getContentFiles().size();
        final var BAG_FILES_COUNT = bag.getBagFiles().size();;
        Assertions.assertThat(BAG_FILES_COUNT).isEqualTo(CONTENT_FILES_COUNT);
      }

      @Test
      public void bagFilesHaveExactlySameDsidsAsContentFilesInSipJson() {
        var expectedDsids = bag.getBagitSipJson().getContentFiles().stream().map(cf -> cf.getDsid()).toList();
        var actualDsids = bag.getBagFiles().stream().map(BagFile::getDsid).toList();
        Assertions.assertThat(actualDsids).containsExactlyInAnyOrderElementsOf(expectedDsids);
      }

      @Test
      public void bagFilesHaveNonNullAndNonEmptyProperties() {
        for(var bagFile : bag.getBagFiles()) {
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
        for(var bagFile : bag.getBagFiles()) {
          Assertions.assertThat(bagFile.getMd5Checksum()).isNotNull().isNotEmpty().hasSize(32);
          Assertions.assertThat(bagFile.getSha512Checksum()).isNotNull().isNotEmpty().hasSize(128);
        }
      }

      @Test
      public void firstBagFileHasExpectedDsid(){
        var firstBagFile = bag.getBagFiles().get(0);
        var firstSipJsonContentFile = bag.getBagitSipJson().getContentFiles().iterator().next();
        Assertions.assertThat(firstBagFile.getDsid()).isEqualTo(firstSipJsonContentFile.getDsid());
      }

      @Test
      public void bagFilesAndSipJsonContentFilesHaveExpectedSameProperties(){
        int index = 0;
        for (var bagContentFile : bag.getBagitSipJson().getContentFiles()) {

          var expectedDsid = bagContentFile.getDsid();
          var actualDsid = bag.getBagFiles().get(index).getDsid();
          Assertions.assertThat(actualDsid).isEqualTo(expectedDsid);

          var expectedTitle = bagContentFile.getTitle();
          var actualTitle = bag.getBagFiles().get(index).getTitle();
          Assertions.assertThat(actualTitle).isEqualTo(expectedTitle);

          var expectedDescription = bagContentFile.getDescription();
          var actualDescription = bag.getBagFiles().get(index).getDescription();
          Assertions.assertThat(actualDescription).isEqualTo(expectedDescription);

          var expectedCreator = bagContentFile.getCreator();
          var actualCreator = bag.getBagFiles().get(index).getCreator();
          Assertions.assertThat(actualCreator).isEqualTo(expectedCreator);

          var expectedRights = bagContentFile.getRights();
          var actualRights = bag.getBagFiles().get(index).getRights();
          Assertions.assertThat(actualRights).isEqualTo(expectedRights);

          var expectedSize = bagContentFile.getSize();
          var actualSize = bag.getBagFiles().get(index).getSize();
          Assertions.assertThat(actualSize).isEqualTo(expectedSize);

          var expectedMimetype = bagContentFile.getMimetype();
          var actualMimetype = bag.getBagFiles().get(index).getMimetype();
          Assertions.assertThat(actualMimetype).isEqualTo(expectedMimetype);

          var expectedBagpath = bagContentFile.getBagpath();
          var actualBagpath = bag.getBagFiles().get(index).getBagpath();
          Assertions.assertThat(actualBagpath).isEqualTo(expectedBagpath);

          var expectedTags = bagContentFile.getTags();
          var actualTags = bag.getBagFiles().get(index).getTags();
          Assertions.assertThat(actualTags).containsExactlyInAnyOrderElementsOf(expectedTags);

          var expectedLang = bagContentFile.getLang();
          var actualLang = bag.getBagFiles().get(index).getLang();
          Assertions.assertThat(actualLang).containsExactlyInAnyOrderElementsOf(expectedLang);

          index++;
        }
      }

    }
  }

}
