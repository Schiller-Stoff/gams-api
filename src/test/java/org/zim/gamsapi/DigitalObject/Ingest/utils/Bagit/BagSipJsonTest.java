package org.zim.gamsapi.DigitalObject.Ingest.utils.Bagit;

import jakarta.validation.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.zim.gamsapi.DigitalObject.Ingest.utils.Bagit.mapping.BagSipJsonContentFile;
import org.zim.gamsapi.DigitalObject.Ingest.utils.Bagit.mapping.BagSipJson;
import org.zim.gamsapi.UnitTest;


public class BagSipJsonTest extends UnitTest {

  BagSipJson bagSipJson;

  ValidatorFactory validatorFactory;
  Validator validator;

  @BeforeEach
  public void setUp() {
    bagSipJson = new BagSipJson();
    bagSipJson.setRecid("id");
    bagSipJson.setProject("project");
    bagSipJson.setTitle("title");
    bagSipJson.setObjectType("objectType");
    bagSipJson.setDescription("description");
    bagSipJson.setCreator("creator");
    bagSipJson.setRights("rights");
    bagSipJson.setPublisher("publisher");
    bagSipJson.setSource("source");
    bagSipJson.setSchema("schema");
    bagSipJson.setCreated_by("created_by");

    // instantiate validator per test
    validatorFactory = jakarta.validation.Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();

  }

  @AfterEach
  public void tearDown(){
    validatorFactory.close();
  }


  @Nested
  public class Validation {

    @Test
    public void testBagitIsValid() {
      // when
      var violations = validator.validate(bagSipJson);
      // then
      Assertions.assertThat(violations).isEmpty();
    }

    @Test
    public void containsOneConstraintViolationIfPublisherIsNull() {
      // given
      bagSipJson.setPublisher(null);
      // when
      var violations = validator.validate(bagSipJson);
      // then
      Assertions.assertThat(violations)
          .isNotEmpty()
          .hasSize(1);
    }

    @Test
    public void containsTwoConstraintViolationsIfRightsAndPublisherAreEmpty() {
      // given
      bagSipJson.setPublisher("");
      bagSipJson.setRights("");
      // when
      var violations = validator.validate(bagSipJson);
      // then
      Assertions.assertThat(violations)
          .isNotEmpty()
          .hasSize(2);
    }


    @Test
    public void containsThreeConstraintViolationsIfContainedBagitContentFileMissesThreeRequiredProperties(){

      // given
      BagSipJsonContentFile bagSipJsonContentFile = new BagSipJsonContentFile();
      // the three missing required properties
      //bagitContentFile.setDsid("dsid");
      //bagitContentFile.setTitle("title");
      bagSipJsonContentFile.setMimetype("mimetype");
      bagSipJsonContentFile.setBagpath("path");
      bagSipJsonContentFile.setRights("rights");
      bagSipJsonContentFile.setSize(0L);
      bagSipJsonContentFile.setCreator("creator");

      bagSipJson.getContentFiles().add(bagSipJsonContentFile);

      // when
      var violations = validator.validate(bagSipJson);
      // then
      Assertions.assertThat(violations)
          .isNotEmpty()
          .hasSize(2);


    }



  }



}
