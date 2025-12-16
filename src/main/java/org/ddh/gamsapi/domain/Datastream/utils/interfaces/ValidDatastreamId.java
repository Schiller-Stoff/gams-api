package org.ddh.gamsapi.domain.Datastream.utils.interfaces;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.ddh.gamsapi.domain.Datastream.utils.validation.DatastreamIdValidator;
import org.ddh.gamsapi.domain.DigitalObject.utils.validation.DigitalObjectIdValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = DatastreamIdValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDatastreamId {

  /**
   * The error message that will be returned when validation fails.
   *
   * @return the error message template
   */
  String message() default "Datastream dsid violates business rules";

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
