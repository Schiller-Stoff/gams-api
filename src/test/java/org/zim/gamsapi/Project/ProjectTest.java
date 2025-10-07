package org.zim.gamsapi.Project;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.UnitTest;
import org.zim.gamsapi.enums.TestProject;
import java.util.Set;

public class ProjectTest extends UnitTest {


  @Nested
  public class IdentityTest {

    @Test
    public void twoProjectsWithSameProjectAbbr_aresConsideredEqual() {

      // given
      final String PROJECT_ABBR = "projectAbbr";

      Project project1 = ProjectBuilder.builder().projectAbbr(PROJECT_ABBR).build();
      Project project2 = ProjectBuilder.builder().projectAbbr(PROJECT_ABBR).build();

      // then
      Assertions.assertEquals(project1, project2);

    }


  }


  @Nested
  public class Validation {


    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void init() {
      validatorFactory = jakarta.validation.Validation.buildDefaultValidatorFactory();
      validator = validatorFactory.getValidator();
    }

    @Test
    public void staticTestProjectProducesNoConstraintViolations() {

      // given
      Project project = TestProject.generate();
      Set<ConstraintViolation<Project>> violations = validator.validate(project);
      org.assertj.core.api.Assertions.assertThat(violations).isEmpty();
    }

    @Test
    public void shouldRaiseConstraintViolationIfProjectAbbrIsNull() {

      // given
      Project project = TestProject.generate();
      project.setProjectAbbr(null);

      // when
      Set<ConstraintViolation<Project>> violations = validator.validate(project);

      // then
      org.assertj.core.api.Assertions.assertThat(violations).hasSize(1);
    }

    @Test
    public void shouldRaiseConstraintViolationIfProjectAbbrIsTooShort() {

      // given
      Project project = TestProject.generate();
      project.setProjectAbbr("a");

      // when
      Set<ConstraintViolation<Project>> violations = validator.validate(project);

      // then
      org.assertj.core.api.Assertions.assertThat(violations).hasSize(1);
    }

    @Test
    public void shouldRaiseConstraintViolationIfProjectAbbrIsTooLong() {

      // given
      Project project = TestProject.generate();
      project.setProjectAbbr("12345678901");

      // when
      Set<ConstraintViolation<Project>> violations = validator.validate(project);

      // then
      org.assertj.core.api.Assertions.assertThat(violations).hasSize(1);
    }


    @Test
    public void shouldRaiseConstraintViolationIfProjectAbbrContainsInvalidCharacters() {

      // given
      Project project = TestProject.generate();
      project.setProjectAbbr("aBcD1@");

      // when
      Set<ConstraintViolation<Project>> violations = validator.validate(project);

      // then
      org.assertj.core.api.Assertions.assertThat(violations).hasSize(1);
    }

  }
}
