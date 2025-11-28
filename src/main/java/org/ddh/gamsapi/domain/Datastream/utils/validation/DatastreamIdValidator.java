package org.ddh.gamsapi.domain.Datastream.utils.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.Datastream.Datastream;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.ValidDatastreamId;

@Slf4j
public class DatastreamIdValidator implements ConstraintValidator<ValidDatastreamId, Datastream> {

  private final static String DSID_PATTERN = "^[a-zA-Z0-9][a-zA-Z0-9._-]*$";

  @Override
  public void initialize(ValidDatastreamId constraintAnnotation) {
    // No initialization needed
    ConstraintValidator.super.initialize(constraintAnnotation);
  }

  @Override
  public boolean isValid(Datastream datastream, ConstraintValidatorContext context) {
    // Implement validation logic for Datastream ID here

    if(datastream == null) {
      String msg = "Datastream is null";
      log.trace(msg);
      addViolation(context, msg);
      return false;
    }

    if(datastream.getDsid() == null || datastream.getDsid().isEmpty()){
      String msg = "Datastream dsid is null or empty";
      addViolation(context, msg);
      log.trace(msg);
      return false;
    }

    final String DSID = datastream.getDsid();

    if(DSID.contains("..")){
      String msg = "Datastream dsid contains invalid sequence '..'";
      addViolation(context, msg);
      log.trace(msg);
      return false;
    }

    if(DSID.contains("--")){
      String msg = "Datastream dsid contains invalid sequence '--'";
      addViolation(context, msg);
      log.trace(msg);
      return false;
    }

    if(DSID.contains("__")){
      String msg = "Datastream dsid contains invalid sequence '__'";
      addViolation(context, msg);
      log.trace(msg);
      return false;
    }

    // additionally ignore case
    if(!DSID.matches(DSID_PATTERN)){
      String msg = "Datastream dsid does not match required pattern: " + DSID_PATTERN;
      addViolation(context, msg);
      log.trace(msg);
      return false;
    }

    return true;
  }


  /**
   * Adds a custom constraint violation with a specific error message.
   * The violation is added to the "id" property path.
   *
   * @param context the constraint validator context
   * @param message the error message
   */
  private void addViolation(ConstraintValidatorContext context, String message) {
    context.buildConstraintViolationWithTemplate(message)
        .addPropertyNode("dsid")
        .addConstraintViolation();

    log.debug("Validation failed: {}", message);
  }

}
