package org.zim.gamsapi.Ingest.utils.Bagit;

import jakarta.validation.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.zim.gamsapi.UnitTest;


public class BagitSipJsonTest extends UnitTest {

  BagitSipJson bagitSipJson;

  ValidatorFactory validatorFactory;
  Validator validator;

  @BeforeEach
  public void setUp() {
    bagitSipJson = new BagitSipJson();
    bagitSipJson.setId("id");
    bagitSipJson.setProject("project");
    bagitSipJson.setTitle("title");
    bagitSipJson.setObjectType("objectType");
    bagitSipJson.setDescription("description");
    bagitSipJson.setCreator("creator");
    bagitSipJson.setRights("rights");
    bagitSipJson.setPublisher("publisher");
    bagitSipJson.setParent("parent");

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
      var violations = validator.validate(bagitSipJson);
      // then
      Assertions.assertThat(violations).isEmpty();
    }

    @Test
    public void containsOneConstraintViolationIfPublisherIsNull() {
      // given
      bagitSipJson.setPublisher(null);
      // when
      var violations = validator.validate(bagitSipJson);
      // then
      Assertions.assertThat(violations)
          .isNotEmpty()
          .hasSize(1);
    }

    @Test
    public void containsTwoConstraintViolationsIfRightsAndPublisherAreEmpty() {
      // given
      bagitSipJson.setPublisher("");
      bagitSipJson.setRights("");
      // when
      var violations = validator.validate(bagitSipJson);
      // then
      Assertions.assertThat(violations)
          .isNotEmpty()
          .hasSize(2);
    }


    @Test
    public void containsThreeConstraintViolationsIfContainedBagitContentFileMissesThreeRequiredProperties(){

      // given
      BagitContentFile bagitContentFile = new BagitContentFile();
      // the three missing required properties
      //bagitContentFile.setDsid("dsid");
      //bagitContentFile.setTitle("title");
      //bagitContentFile.setDescription("description");
      bagitContentFile.setMimetype("mimetype");
      bagitContentFile.setBagpath("path");
      bagitContentFile.setRights("rights");
      bagitContentFile.setSize(0L);
      bagitContentFile.setPublisher("publisher");
      bagitContentFile.setCreator("creator");

      bagitSipJson.getContentFiles().add(bagitContentFile);

      // when
      var violations = validator.validate(bagitSipJson);
      // then
      Assertions.assertThat(violations)
          .isNotEmpty()
          .hasSize(3);


    }



  }



}
