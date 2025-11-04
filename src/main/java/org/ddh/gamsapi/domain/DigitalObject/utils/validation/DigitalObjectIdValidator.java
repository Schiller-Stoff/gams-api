package org.ddh.gamsapi.domain.DigitalObject.utils.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;

/**
 * Validator for the {@link ValidDigitalObjectId} annotation.
 * Implements the business rules for validating digital object IDs.
 * Note: This validator does not check for the uniqueness of the ID.
 * Uniqueness should be enforced at the database level.
 */
@Slf4j
public class DigitalObjectIdValidator implements ConstraintValidator<ValidDigitalObjectId, DigitalObject> {

  private static final String LOCAL_ID_PATTERN = "^[a-z0-9][a-z0-9.-]*$";

  @Override
  public void initialize(ValidDigitalObjectId constraintAnnotation) {
    // No initialization needed
    ConstraintValidator.super.initialize(constraintAnnotation);
  }

  /**
   * Validates a digital object's ID according to GAMS business rules.
   *
   * @param digitalObject the digital object to validate (can be null)
   * @param context       the constraint validator context
   * @return {@code true} if valid or if null (let @NotNull handle that case), {@code false} otherwise
   */
  @Override
  public boolean isValid(DigitalObject digitalObject, ConstraintValidatorContext context) {
    if (digitalObject == null) {
      String msg = "Digital object is null";
      log.trace(msg);
      addViolation(context, msg);
      return false;
    }

    String id = digitalObject.getId();

    if (id == null || id.isEmpty()) {
      String msg = "Digital object ID is null or empty";
      addViolation(context, msg);
      log.trace(msg);
      return false;
    }

    // If project is null
    if (digitalObject.getProject() == null) {
      String msg = "Digital object project is null or empty";
      log.trace(msg);
      addViolation(context, msg);
      return false;
    }

    String projectAbbr = digitalObject.getProject().getProjectAbbr();

    // If project abbreviation is null, let project validation handle it
    if (projectAbbr == null || projectAbbr.isEmpty()) {
      String msg = String.format("Digital object project abbr is null or empty. %s", digitalObject);
      log.trace(msg);
      addViolation(context, msg);
      return false;
    }

    // Disable default constraint violation message
    context.disableDefaultConstraintViolation();

    int minLength = 5;
    if(id.length() < minLength){
      String msg = String.format("Digital object id is too short (shorter than %s). Got id: %s",minLength,  digitalObject.getId());
      addViolation(context, msg);
      return false;
    }

    int maxLength = 64;
    if(id.length() > maxLength){
      String msg = String.format("Digital object id is too long (bigger than %s). Got id: %s", maxLength, digitalObject.getId());
      addViolation(context, msg);
      return false;
    }

    // e.g. o:derla.sty256
    // o: -> type prefix
    // derla -> project abbreviation
    // sty256 -> local identifier

    // Extract the part after optional type prefix (e.g., "o:")
    String projectAbbrWithLocalId = extractAfterTypePrefix(id);

    // Rule 1: Must start with project abbreviation + dot
    if (!validateProjectPrefix(projectAbbrWithLocalId, projectAbbr, context)) {
      String msg = String.format("Digital object id does not start with the expected project abbreviation. Expected to start with: %s - but got: %s",
          projectAbbr + ".", projectAbbrWithLocalId);
      log.trace(msg);
      addViolation(context, msg);
      return false;
    }

    // Extract local ID part (everything after "project-abbr.")
    String localId = projectAbbrWithLocalId.substring(projectAbbr.length() + 1);

    // Rule 2: No consecutive dots
    if (localId.contains("..")) {
      String msg = String.format("Digital object id contains consecutive dots. Digital object id: %s", digitalObject.getId());
      log.trace(msg);
      addViolation(context, msg);
      return false;
    }

    // Rule 3: No underscores (per documentation)
    if (localId.contains("_")) {
      String msg = String.format("Digital object id contains underscores (_). Digital object id: %s", digitalObject.getId());
      log.trace(msg);
      addViolation(context, msg);
      return false;
    }

    // Rule 4: No consecutive dashes
    if (localId.contains("--")) {
      String msg = String.format("Digital object id contains consecutive dashes (--). Digital object id: %s", digitalObject.getId());
      log.trace(msg);
      addViolation(context, msg);
      return false;
    }

    // Rule 5: Local ID must match pattern (starts with letter/number, followed by valid chars)
    if (!localId.matches(LOCAL_ID_PATTERN)) {
      String msg = String.format("Digital object id contains invalid characters. Digital object id: %s", digitalObject.getId());
      log.trace(msg);
      addViolation(context, msg);
      return false;
    }

    // All validation rules passed
    log.debug("Digital object ID '{}' passed all validation rules", id);
    return true;
  }

  /**
   * Extracts the part after the optional type prefix.
   * If the ID contains a colon (legacy format like "o:test.123"), returns everything after the colon.
   * Otherwise, returns the ID as-is.
   *
   * @param id the digital object ID
   * @return the ID without type prefix
   */
  private String extractAfterTypePrefix(String id) {
    if (id.contains(":")) {
      return id.substring(id.indexOf(":") + 1);
    }
    return id;
  }

  /**
   * Validates that the ID starts with the expected project abbreviation followed by a dot.
   *
   * @param idPart      the ID part to validate (without type prefix)
   * @param projectAbbr the expected project abbreviation
   * @param context     the constraint validator context
   * @return {@code true} if valid, {@code false} otherwise
   */
  private boolean validateProjectPrefix(String idPart, String projectAbbr, ConstraintValidatorContext context) {
    String expectedPrefix = projectAbbr + ".";

    if (!idPart.startsWith(expectedPrefix)) {
      addViolation(context, String.format(
          "Digital object ID must start with project abbreviation '%s' followed by a dot. " +
              "Expected to start with: '%s' but got: '%s'",
          projectAbbr, expectedPrefix, idPart
      ));
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
        .addPropertyNode("id")
        .addConstraintViolation();

    log.debug("Validation failed: {}", message);
  }

}
