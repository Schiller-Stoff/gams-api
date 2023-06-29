package org.zim.gamsapi.DigitalObject;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.MetadataBaseEntity;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConstraintViolationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }


    @Test
    public void shouldRaiseNoConstraintViolation() {
        DigitalObject digitalObject = new DigitalObject("foo", null, "", "", null);

        Set<ConstraintViolation<DigitalObject>> violationSet = validator.validate(digitalObject);
        assertThat(violationSet, is(empty()));
    }

    @Test
    public void shouldRaiseContraintViolationIfPidIsNull() {
        DigitalObject digitalObject = new DigitalObject(null, null, "", "", null);

        Set<ConstraintViolation<DigitalObject>> violationSet = validator.validate(digitalObject);
        assertThat(violationSet.size(), is(1));
    }


    @Test
    public void shouldRaiseConstraintViolationIfMetadataIsEmpty() {
        MetadataBaseEntity metadataBaseEntity = new MetadataBaseEntity();

        Set<ConstraintViolation<MetadataBaseEntity>> violationSet = validator.validate(metadataBaseEntity);
        assertThat(violationSet.size(), is(3));
    }
}
