package org.zim.gamsapi.domain.Datastream;

import jakarta.validation.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.UnitTest;
import org.zim.gamsapi.TestUtilities.TestDatastream;
import org.zim.gamsapi.TestUtilities.TestDigitalObject;
import org.zim.gamsapi.TestUtilities.TestMetadataBaseEntity;
import org.zim.gamsapi.domain.Datastream.Datastream;
import org.zim.gamsapi.domain.Datastream.DatastreamId;

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
          TestDigitalObject.generate()
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
          TestDigitalObject.generate()
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
          TestDigitalObject.generate()
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

    @BeforeAll
    static void init() {
      validatorFactory = Validation.buildDefaultValidatorFactory();
      validator = validatorFactory.getValidator();
    }

    @Test
    public void testDatastreamShouldNotRaiseAnyConstraintViolation() {
      Datastream datastream = TestDatastream.generate();

      Set<ConstraintViolation<Datastream>> violationSet = validator.validate(datastream);
      org.assertj.core.api.Assertions.assertThat(violationSet).isEmpty();
    }


    @Test
    public void testDatastreamShouldThrowIfDsidIsNull() {
      Datastream datastream = TestDatastream.generate();
      datastream.setDsid(null);

      Assertions.assertThrows(ValidationException.class, () -> validator.validate(datastream));
    }

    @Test
    public void testDatastreamShouldRaiseConstraintViolationIfDigitalObjectIsNull() {
      Datastream datastream = TestDatastream.generate();
      datastream.setDigitalObject(null);

      Set<ConstraintViolation<Datastream>> violationSet = validator.validate(datastream);
      org.assertj.core.api.Assertions.assertThat(violationSet.size()).isEqualTo(1);
    }

    @Test
    public void testDatastreamShouldRaiseConstraintViolationIfSizeIsNull() {
      Datastream datastream = TestDatastream.generate();
      datastream.setSize(null);

      Set<ConstraintViolation<Datastream>> violationSet = validator.validate(datastream);
      org.assertj.core.api.Assertions.assertThat(violationSet.size()).isEqualTo(1);
    }

    @Test
    public void testDatastreamShouldRaiseConstraintViolationIfMimeTypeIsNull() {
      Datastream datastream = TestDatastream.generate();
      datastream.setMimeType(null);

      Set<ConstraintViolation<Datastream>> violationSet = validator.validate(datastream);
      org.assertj.core.api.Assertions.assertThat(violationSet.size()).isEqualTo(1);
    }


    @Test
    public void raisesNoViolationsIfDsidContainsUnderscore(){
      Datastream datastream = TestDatastream.generate();
      datastream.setDsid("_test.xml");
      Set<ConstraintViolation<Datastream>> violationSet = validator.validate(datastream);
      org.assertj.core.api.Assertions.assertThat(violationSet.size()).isEqualTo(0);
    }

    @Test
    public void raisesViolationIfDsidContainsSpecialCharacter(){
      Datastream datastream = TestDatastream.generate();
      datastream.setDsid("test!.xml");
      Set<ConstraintViolation<Datastream>> violationSet = validator.validate(datastream);
      org.assertj.core.api.Assertions.assertThat(violationSet.size()).isEqualTo(1);
    }

    @Test
    public void raisesViolationIfDsidContainsSpace(){
      Datastream datastream = TestDatastream.generate();
      datastream.setDsid("test test.xml");
      Set<ConstraintViolation<Datastream>> violationSet = validator.validate(datastream);
      org.assertj.core.api.Assertions.assertThat(violationSet.size()).isEqualTo(1);
    }

    @Test
    public void raisesViolationIfDsidContainsNoDot(){
      Datastream datastream = TestDatastream.generate();
      datastream.setDsid("test");
      Set<ConstraintViolation<Datastream>> violationSet = validator.validate(datastream);
      org.assertj.core.api.Assertions.assertThat(violationSet.size()).isEqualTo(1);
    }




  }



}
