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
  }





}
