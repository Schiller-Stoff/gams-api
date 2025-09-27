package org.zim.gamsapi.Ingest.utils.converter;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.Ingest.utils.Bagit.mapping.BagSipJson;
import org.zim.gamsapi.UnitTest;
import java.util.Set;

public class BagSipJsonDigitalObjectConverterTest extends UnitTest {


  @Test
  public void convertsExpectedBagitSipJson(){

    BagSipJson bagSipJson = new BagSipJson();
    bagSipJson.setId("testId");
    bagSipJson.setProject("testProject");
    bagSipJson.setObjectType("testObjectType");
    bagSipJson.setTypes(Set.of("testTypes"));
    bagSipJson.setTitle("testTitle");
    bagSipJson.setCreator("testCreator");
    bagSipJson.setDescription("testDescription");
    bagSipJson.setRights("testRights");
    bagSipJson.setPublisher("testPublisher");
    bagSipJson.setFunder("testFunder");
    bagSipJson.setMainResource("testMainResource");

    BagitSipJsonDigitalObjectConverter bagitSipJsonDigitalObjectConverter = new BagitSipJsonDigitalObjectConverter();
    DigitalObject convertedDigitalObject = bagitSipJsonDigitalObjectConverter.convert(bagSipJson);

    Assertions.assertThat(convertedDigitalObject).isNotNull();
    Assertions.assertThat(convertedDigitalObject.getId()).isEqualTo(bagSipJson.getId());
    Assertions.assertThat(convertedDigitalObject.getProject().getProjectAbbr()).isEqualTo(bagSipJson.getProject());
    Assertions.assertThat(convertedDigitalObject.getObjectType()).isEqualTo(bagSipJson.getObjectType());
    Assertions.assertThat(convertedDigitalObject.getBaseMetadata().getTitle()).isEqualTo(bagSipJson.getTitle());
    Assertions.assertThat(convertedDigitalObject.getBaseMetadata().getCreator()).isEqualTo(bagSipJson.getCreator());
    Assertions.assertThat(convertedDigitalObject.getBaseMetadata().getDescription()).isEqualTo(bagSipJson.getDescription());
    Assertions.assertThat(convertedDigitalObject.getBaseMetadata().getRights()).isEqualTo(bagSipJson.getRights());
    Assertions.assertThat(convertedDigitalObject.getPublisher()).isEqualTo(bagSipJson.getPublisher());
    Assertions.assertThat(convertedDigitalObject.getMainResource()).isEqualTo(bagSipJson.getMainResource());

    Assertions.assertThat(convertedDigitalObject.getFunder()).isEqualTo(bagSipJson.getFunder());
  }

  @Test
  public void throwsIllegalStateIfObjectIdWouldBeNull(){

    BagSipJson bagSipJson = new BagSipJson();

    BagitSipJsonDigitalObjectConverter bagitSipJsonDigitalObjectConverter = new BagitSipJsonDigitalObjectConverter();

    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> bagitSipJsonDigitalObjectConverter.convert(bagSipJson));
  }


}
