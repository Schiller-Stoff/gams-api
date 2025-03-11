package org.zim.gamsapi.DigitalObject;

import jakarta.validation.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.UnitTest;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestMetadataBaseEntity;
import java.util.Set;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ConstraintViolationTest extends UnitTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }


    @Test
    public void shouldRaiseNoConstraintViolation() {
        DigitalObject digitalObject = TestDigitalObject.generate();
        Set<ConstraintViolation<DigitalObject>> violationSet = validator.validate(digitalObject);
        assertThat(violationSet, is(empty()));
    }

    @Test
    public void shouldRaiseValidationExceptionIfIdIsNull() {
        DigitalObject digitalObject = TestDigitalObject.generate();
        digitalObject.setId(null);
        Assertions.assertThrows(ValidationException.class, () -> validator.validate(digitalObject));
    }


    @Test
    public void shouldRaiseConstraintViolationIfMetadataIsEmpty() {
        MetadataBaseEntity metadataBaseEntity = new MetadataBaseEntity();
        Set<ConstraintViolation<MetadataBaseEntity>> violationSet = validator.validate(metadataBaseEntity);
        assertThat(violationSet.size(), is(3));
    }

    @Test
    public void shouldNotRaiseConstraintViolationIfMetadataDescriptionIsAnEmptyString() {
        MetadataBaseEntity metadataBaseEntity = TestMetadataBaseEntity.generate();
        // set a description that is too short
        metadataBaseEntity.setDescription("");
        Set<ConstraintViolation<MetadataBaseEntity>> violationSet = validator.validate(metadataBaseEntity);
        assertThat(violationSet.size(), is(0));
    }

    @Test
    public void shouldNotRaiseConstraintViolationIfMetadataDescriptionIsNull() {
        MetadataBaseEntity metadataBaseEntity = TestMetadataBaseEntity.generate();
        // set a description that is too short
        metadataBaseEntity.setDescription(null);
        Set<ConstraintViolation<MetadataBaseEntity>> violationSet = validator.validate(metadataBaseEntity);
        assertThat(violationSet.size(), is(0));
    }

    @Test
    public void shouldRaiseConstraintViolationIfMetadataCreatorIsNull() {
        MetadataBaseEntity metadataBaseEntity = TestMetadataBaseEntity.generate();
        // set creator to null
        metadataBaseEntity.setCreator(null);
        Set<ConstraintViolation<MetadataBaseEntity>> violationSet = validator.validate(metadataBaseEntity);
        assertThat(violationSet.size(), is(1));
    }

    @Test
    public void raisesConstraintViolationIfProjectAbbrIsNotContainedInId(){

        DigitalObject digitalObject = new DigitalObjectBuilder()
            .project(Project.builder().projectAbbr("foo").build())
            .id("foo")
            .publisher("foo")
            .baseMetadata(TestMetadataBaseEntity.generate())
            .build();

        Set<ConstraintViolation<DigitalObject>> violationSet = validator.validate(digitalObject);
        assertThat(violationSet, not(empty()));

    }

    @Test
    public void shouldRaiseOneConstraintViolationIfIdIsTooLong(){
        DigitalObject digitalObject = TestDigitalObject.generate();
        digitalObject.setId(digitalObject.getId() + "12345678901312312321312312323"); //
        Set<ConstraintViolation<DigitalObject>> violationSet = validator.validate(digitalObject);
        assertThat(violationSet.size(), is(1));
    }

    @Test
    public void shouldRaiseConstraintViolationIfIdContains$(){
        DigitalObject digitalObject = TestDigitalObject.generate();
        digitalObject.setId(digitalObject.getId() + "$");
        Set<ConstraintViolation<DigitalObject>> violationSet = validator.validate(digitalObject);
        assertThat(violationSet.size(), is(1));
    }

    @Test
    public void doesNotRaiseConstraintViolationIfIdContainsDots(){
        DigitalObject digitalObject = TestDigitalObject.generate();
        digitalObject.setId(digitalObject.getId() + "...");
        Set<ConstraintViolation<DigitalObject>> violationSet = validator.validate(digitalObject);
        assertThat(violationSet, is(empty()));
    }

    @Test
    public void shouldRaiseIfIdContainsUppercasedAChar(){
        DigitalObject digitalObject = TestDigitalObject.generate();
        digitalObject.setId(digitalObject.getId() + "A");
        Set<ConstraintViolation<DigitalObject>> violationSet = validator.validate(digitalObject);
        assertThat(violationSet.size(), is(1));
    }

}
