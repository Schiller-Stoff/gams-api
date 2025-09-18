package org.zim.gamsapi.Ingest.utils.Bagit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
  }





}
