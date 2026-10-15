package org.ddh.gamsapi.domain.ArchivalRecord;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
public class ArchivalRecordPidStringValidator implements ConstraintValidator<ValidPid, String> {

  /**
   * Combined pre-compiled pattern for Handles and DOIs:
   * - Handle prefix: "hdl:" followed by numeric naming authority (e.g., 11471 or 21.11115)
   * - DOI prefix: "doi:10." followed by a 4-to-9 digit registrant code (with optional sub-elements)
   * - Suffix (after '/'): Starts with alphanumeric, followed by alphanumeric, '.', '_', or '-'
   */
  private static final String PID_REGEX =
      "^(?:hdl:[0-9]+(?:\\.[0-9]+)*|doi:10\\.[0-9]{4,9}(?:\\.[0-9]+)*)/[a-zA-Z0-9][a-zA-Z0-9._-]*$";

  private static final Pattern COMPILED_PID_PATTERN = Pattern.compile(PID_REGEX);

  private static final int MIN_LENGTH = 8;
  private static final int MAX_LENGTH = 255;

  @Override
  public boolean isValid(String pid, ConstraintValidatorContext context) {
    // Always disable default violation first so custom messages don't duplicate
    context.disableDefaultConstraintViolation();

    if (pid == null || pid.isBlank()) {
      addViolation(context, "PID must not be null or blank.");
      return false;
    }

    if (pid.startsWith("/")) {
      addViolation(context, "PID must not start with '/' - Invalid PID: " + pid);
      return false;
    }

    if (!pid.startsWith("hdl:") && !pid.startsWith("doi:")) {
      addViolation(context, "PID must start with 'hdl:' or 'doi:' - Invalid PID: " + pid);
      return false;
    }

    if (pid.length() < MIN_LENGTH) {
      addViolation(context, "PID is too short (minimum " + MIN_LENGTH + " characters). Got: " + pid);
      return false;
    }

    if (pid.length() > MAX_LENGTH) {
      addViolation(context, "PID is too long (maximum " + MAX_LENGTH + " characters). Got: " + pid);
      return false;
    }

    if (pid.contains("..") || pid.contains("--") || pid.contains("__")) {
      addViolation(context, "PID contains an invalid repeated sequence ('..', '--', or '__'): " + pid);
      return false;
    }

    if (!COMPILED_PID_PATTERN.matcher(pid).matches()) {
      addViolation(
          context,
          "PID does not match required Handle or DOI format (pattern: " + PID_REGEX + "). Got: " + pid
      );
      return false;
    }

    return true;
  }

  private void addViolation(ConstraintValidatorContext context, String message) {
    context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
  }
}