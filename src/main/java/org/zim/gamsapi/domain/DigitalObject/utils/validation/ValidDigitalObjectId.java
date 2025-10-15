package org.zim.gamsapi.domain.DigitalObject.utils.validation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation to validate digital object IDs according to GAMS business rules.
 * Note: This validator does not check for the uniqueness of the ID.
 * Uniqueness should be enforced at the database level.
 */
@Documented
@Constraint(validatedBy = DigitalObjectIdValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDigitalObjectId {

  /**
   * The error message that will be returned when validation fails.
   *
   * @return the error message template
   */
  String message() default "Digital object ID violates business rules";

  /**
   * Allows specification of validation groups, to which this constraint belongs.
   *
   * @return the groups this constraint belongs to
   */
  Class<?>[] groups() default {};

  /**
   * Can be used by clients of the Bean Validation API to assign custom payload objects to a constraint.
   *
   * @return the payload associated to this constraint
   */
  Class<? extends Payload>[] payload() default {};

}
