package org.zim.gamsapi.DigitalObject;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

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
        assertTrue(violationSet.isEmpty());
    }

    @Test
    public void shouldRaiseContraintViolationIfPidIsNull() {
        DigitalObject digitalObject = new DigitalObject(null, null, "", "", null);

        Set<ConstraintViolation<DigitalObject>> violationSet = validator.validate(digitalObject);
        assertEquals(1, violationSet.size());
    }
}
