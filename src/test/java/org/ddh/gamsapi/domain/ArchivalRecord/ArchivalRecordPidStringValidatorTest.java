package org.ddh.gamsapi.domain.ArchivalRecord;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ArchivalRecordPidStringValidatorTest {

  private ArchivalRecordPidStringValidator validator;
  private ConstraintValidatorContext context;

  @BeforeEach
  void setUp() {
    validator = new ArchivalRecordPidStringValidator();
    context = Mockito.mock(ConstraintValidatorContext.class);
    ConstraintValidatorContext.ConstraintViolationBuilder builder =
        Mockito.mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
    when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "hdl:11471/1010",
      "hdl:21.11115/0000-000B-D8A9-3",
      "doi:10.5281/zenodo.23314128",
      "doi:10.1000/182",
      "doi:10.1038.12/nature12373"
  })
  void validPidsShouldPass(String validPid) {
    assertThat(validator.isValid(validPid, context)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "",
      "   ",
      "/hdl:11471/1010",
      "urn:nbn:de:1010",              // Unsupported scheme
      "doi:11.5281/zenodo.23314128",  // Invalid DOI directory indicator (must be 10.)
      "doi:10.123/zenodo.1",          // Registrant code < 4 digits
      "doi:10.5281/zenodo..23314128", // Consecutive dots
      "doi:10.5281/.hidden",          // Suffix doesn't start with alphanumeric
      "hdl:11471/"                    // Missing suffix
  })
  void invalidPidsShouldFail(String invalidPid) {
    assertThat(validator.isValid(invalidPid, context)).isFalse();
  }
}