package org.zim.gamsapi.Ingest.utils.converter;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.Ingest.utils.Bagit.BagitSipJson;
import org.zim.gamsapi.UnitTest;

import java.util.Set;

public class BagitSipJsonDigitalObjectConverterTest extends UnitTest {


  @Test
  public void convertsExpectedBagitSipJson(){

    BagitSipJson bagitSipJson = new BagitSipJson();
    bagitSipJson.setId("testId");
    bagitSipJson.setProject("testProject");
    bagitSipJson.setObjectType("testObjectType");
    bagitSipJson.setParent("testParent");
    bagitSipJson.setTypes(Set.of("testTypes"));
    bagitSipJson.setTitle("testTitle");
    bagitSipJson.setCreator("testCreator");
    bagitSipJson.setDescription("testDescription");
    bagitSipJson.setRights("testRights");
    bagitSipJson.setPublisher("testPublisher");

    BagitSipJsonDigitalObjectConverter bagitSipJsonDigitalObjectConverter = new BagitSipJsonDigitalObjectConverter();
    DigitalObject convertedDigitalObject = bagitSipJsonDigitalObjectConverter.convert(bagitSipJson);

    Assertions.assertThat(convertedDigitalObject).isNotNull();
    Assertions.assertThat(convertedDigitalObject.getId()).isEqualTo(bagitSipJson.getId());
    Assertions.assertThat(convertedDigitalObject.getProject().getProjectAbbr()).isEqualTo(bagitSipJson.getProject());
    Assertions.assertThat(convertedDigitalObject.getObjectType()).isEqualTo(bagitSipJson.getObjectType());
    Assertions.assertThat(convertedDigitalObject.getParent().getId()).isEqualTo(bagitSipJson.getParent());
    Assertions.assertThat(convertedDigitalObject.getTypes()).isEqualTo(bagitSipJson.getTypes());
    Assertions.assertThat(convertedDigitalObject.getBaseMetadata().getTitle()).isEqualTo(bagitSipJson.getTitle());
    Assertions.assertThat(convertedDigitalObject.getBaseMetadata().getCreator()).isEqualTo(bagitSipJson.getCreator());
    Assertions.assertThat(convertedDigitalObject.getBaseMetadata().getDescription()).isEqualTo(bagitSipJson.getDescription());
    Assertions.assertThat(convertedDigitalObject.getBaseMetadata().getRights()).isEqualTo(bagitSipJson.getRights());
    Assertions.assertThat(convertedDigitalObject.getBaseMetadata().getPublisher()).isEqualTo(bagitSipJson.getPublisher());

  }

  @Test
  public void throwsIllegalStateIfObjectIdWouldBeNull(){

    BagitSipJson bagitSipJson = new BagitSipJson();

    BagitSipJsonDigitalObjectConverter bagitSipJsonDigitalObjectConverter = new BagitSipJsonDigitalObjectConverter();

    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> bagitSipJsonDigitalObjectConverter.convert(bagitSipJson));
  }


}
