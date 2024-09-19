package org.zim.gamsapi.Datastream;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DigitalObjectBuilder;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.UnitTest;
import org.zim.gamsapi.enums.TestDatastream;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestMetadataBaseEntity;

import java.util.Set;

public class DatastreamTest extends UnitTest {

  MetadataBaseEntity testMetadataBaseEntity = TestMetadataBaseEntity.generate();

  @Nested
  public class IdentityTests {

    @Test
    public void comparingDatastreamsWithJustSameDsidThrows(){
      Datastream datastream = new Datastream();
      datastream.setDsid(TestDatastream.DSID.getValue());
      Datastream datastream2 = new Datastream();
      datastream2.setDsid(TestDatastream.DSID.getValue());

      Assertions.assertThrows(
          IllegalStateException.class,
          () -> Assertions.assertEquals(datastream, datastream2)
      );
    }

    @Test
    public void datastreamsWithSamdeDsidAndDigitalObjectAreEqual(){
      Datastream datastream = new Datastream();
      datastream.setDsid("dsid");
      datastream.setDigitalObject(
          TestDigitalObject.generate("foo")
      );

      Datastream datastream2 = new Datastream();
      datastream2.setDsid(datastream.getDsid());
      datastream2.setDigitalObject(datastream.getDigitalObject());

      Assertions.assertEquals(datastream, datastream2);

    }


  }

  @Nested
  public class DeriveDatastreamId {

    @Test
    public void deriveDatastreamIdFromDigitalObjectAndDsidReturnsExpectedValues(){
      Datastream datastream = new Datastream();
      datastream.setDsid("dsid");
      datastream.setDigitalObject(
          TestDigitalObject.generate("foo")
      );

      DatastreamId datastreamId = datastream.deriveDatastreamId();
      Assertions.assertEquals(datastream.getDigitalObject().getId(), datastreamId.getDigitalObject());
      Assertions.assertEquals(datastream.getDsid(), datastreamId.getDsid());
    }

    @Test
    public void throwsExceptionWhenDigitalObjectIsNull(){
      Datastream datastream = new Datastream();
      datastream.setDsid("dsid");
      Assertions.assertThrows(
          IllegalStateException.class,
          datastream::deriveDatastreamId
      );
    }

    @Test
    public void throwsExceptionWhenDsidIsNull(){
      Datastream datastream = new Datastream();
      datastream.setDigitalObject(
          TestDigitalObject.generate("foo")
      );
      Assertions.assertThrows(
          IllegalStateException.class,
          datastream::deriveDatastreamId
      );
    }

  }

  @Nested
  public class ValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    private final DigitalObject TEST_DIGITAL_OBJECT = TestDigitalObject.generate("foo");

    @BeforeAll
    static void init() {
      validatorFactory = Validation.buildDefaultValidatorFactory();
      validator = validatorFactory.getValidator();
    }

    @Test
    public void testDatastreamShouldNotRaiseAnyConstraintViolation() {
      Datastream datastream = TestDatastream.generate(
          TEST_DIGITAL_OBJECT
      );

      Set<ConstraintViolation<Datastream>> violationSet = validator.validate(datastream);
      org.assertj.core.api.Assertions.assertThat(violationSet).isEmpty();
    }


    @Test
    public void testDatastreamShouldRaiseConstraintViolationIfDsidIsNull() {
      Datastream datastream = TestDatastream.generate(
          TEST_DIGITAL_OBJECT
      );
      datastream.setDsid(null);

      Set<ConstraintViolation<Datastream>> violationSet = validator.validate(datastream);
      org.assertj.core.api.Assertions.assertThat(violationSet.size()).isEqualTo(1);
    }

    @Test
    public void testDatastreamShouldRaiseConstraintViolationIfDigitalObjectIsNull() {
      Datastream datastream = TestDatastream.generate(
          TEST_DIGITAL_OBJECT
      );
      datastream.setDigitalObject(null);

      Set<ConstraintViolation<Datastream>> violationSet = validator.validate(datastream);
      org.assertj.core.api.Assertions.assertThat(violationSet.size()).isEqualTo(1);
    }

    @Test
    public void testDatastreamShouldRaiseConstraintViolationIfSizeIsNull() {
      Datastream datastream = TestDatastream.generate(
          TEST_DIGITAL_OBJECT
      );
      datastream.setSize(null);

      Set<ConstraintViolation<Datastream>> violationSet = validator.validate(datastream);
      org.assertj.core.api.Assertions.assertThat(violationSet.size()).isEqualTo(1);
    }

    @Test
    public void testDatastreamShouldRaiseConstraintViolationIfMimeTypeIsNull() {
      Datastream datastream = TestDatastream.generate(
          TEST_DIGITAL_OBJECT
      );
      datastream.setMimeType(null);

      Set<ConstraintViolation<Datastream>> violationSet = validator.validate(datastream);
      org.assertj.core.api.Assertions.assertThat(violationSet.size()).isEqualTo(1);
    }


  }



}
