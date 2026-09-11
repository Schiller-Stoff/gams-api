package org.ddh.gamsapi.domain.ArchivalRecord;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ArchivalRecordPidStringValidator implements ConstraintValidator<ValidPid, String> {

  private static final String PID_PATTERN = "^hdl:[0-9]+(\\.[0-9]+)*\\/[a-zA-Z0-9][a-zA-Z0-9._-]*$";
  private static final int MIN_LENGTH = 8;
  private static final int MAX_LENGTH = 255;

  @Override
  public boolean isValid(String pid, ConstraintValidatorContext context) {
    if (pid == null || pid.isEmpty()) {
      addViolation(context, "PID is null or empty");
      return false;
    }

    context.disableDefaultConstraintViolation();

    if (pid.length() < MIN_LENGTH) {
      addViolation(context, "PID is too short (shorter than " + MIN_LENGTH + "). Got: " + pid);
      return false;
    }
    if (pid.length() > MAX_LENGTH) {
      addViolation(context, "PID is too long (longer than " + MAX_LENGTH + "). Got: " + pid);
      return false;
    }
    if (pid.contains("..") || pid.contains("--") || pid.contains("__")) {
      addViolation(context, "PID contains an invalid repeated sequence: " + pid);
      return false;
    }
    if (!pid.matches(PID_PATTERN)) {
      addViolation(context, "PID does not match required pattern: " + PID_PATTERN + ". Got: " + pid);
      return false;
    }
    return true;
  }

  private void addViolation(ConstraintValidatorContext context, String message) {
    context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
  }
}
