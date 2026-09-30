package org.ddh.gamsapi.domain.ArchivalRecord;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Annotation to validate the PID of an ArchivalRecord
 */
@Documented
@Constraint(validatedBy = {ArchivalRecordPidStringValidator.class})
@Target({ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPid {
  String message() default "PID violates business rules";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}