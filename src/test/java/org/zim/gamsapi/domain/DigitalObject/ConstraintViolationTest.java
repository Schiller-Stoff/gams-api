package org.zim.gamsapi.domain.DigitalObject;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.TestUtilities.TestDigitalObject;
import org.zim.gamsapi.TestUtilities.TestMetadataBaseEntity;
import org.zim.gamsapi.UnitTest;
import org.zim.gamsapi.domain.MetadataBaseEntity;
import org.zim.gamsapi.domain.Project.ProjectBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ConstraintViolationTest extends UnitTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Nested
    public class DigitalObjectIdValidation {

      @Test
      public void shouldRaiseNoConstraintViolationForValidIds() {
        // Valid Digital Object IDs - following xml:id rules with modifications
        List<String> validDigitalObjectIds = Arrays.asList(
            // Basic valid formats with project prefix
            "%s.1",
            "%s.123",
            "%s.test1",
            "%s.abc",
            "%s.a1b2c3",

            // With numbers, dots, dashes, underscores after the dot
            "%s.object-1",
            "%s.object.sub",
            "%s.doc-2024-001",
            "%s.item.1.2.3",

            // Different project prefixes (letters only, can contain dashes)
            "%s.1234",
            "%s.doc-001",
            "%s.manuscript-2024",


            // Legacy format with type prefix (discouraged but valid)
            "o:%s.1",
            "o:%s.manuscript-001",
            "o:%s.doc123",
            "p:%s.object.sub",

            // Complex valid IDs
            "%s.collection.subcol.item-001",

            // Edge cases (valid)
            "%s.1",                          // shortest project prefix (1 letter)
            "%s.a",                       // single letter after dot
            "%s.1a",                      // starts with number after dot (valid)
            "%s.123abc",                  // number start after dot
            "%s.a-b-c-d-e",              // multiple dashes
            "%s.a.b.c.d.e",              // multiple dots

            // Real-world examples based on your test data
            "%s.test",
            "%s.manifest",
            "%s.dc-metadata",
            "%s.manuscript-01",
            "%s.tei-document-2024"
        );

        for (String id : validDigitalObjectIds) {
          DigitalObject digitalObject = TestDigitalObject.generate();
          // format the id with the project's abbreviation from the test digital object
          id = String.format(id, digitalObject.getProject().getProjectAbbr());
          digitalObject.setId(id);
          org.assertj.core.api.Assertions.assertThat(
              validator.validate(digitalObject))
              .isEmpty();
        }
      }

      @Test
      public void shouldRaiseConstraintViolationForInvalidIds() {
        String[] invalidIdFragments = {
            // underscores are not allowed
            "%s.object_2",
            "%s.manuscript_123",
            "%s.a_b_c_d_e",
            "%s.a1_b2-c3.d4",
            "%s-project.doc_001",
            "t:%s.item_1",
            "%s.document-2024_v1.final",
            "%s.tei-doc_2024-10-15",
            "%s.123-abc_def.xyz",
            "%s.a1-b2_c3.d4-e5",
            "%s.abc-123_xyz.final",

            // no dot as separator
            "%s-project.item1",
            "%s-proj.123abc",

            ".abcdef%s", // (starts with a dot)
            "1abcdef%s", // (starts with a number)
            "%s.abc/def", // (contains invalid character '/')
            "%s.abc@def", // (contains invalid character '@')
            "%sabcdef", // (no dot)
            "%s.abc..def", // (consecutive dots)

            "%s.ABC123",               // Uppercase letters
            "%s.project1$object2",     // Special character $
            "%s.a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
            "%s.proj ect name",        // Space
            "%s.id#with#hash",         // Special character #
            "%s.id/with/slash",        // Special character /
            "%s.id\\with\\backslash",  // Special character \
            "%s.id,with,comma",        // Special character ,
            "%s.id;with:semicolon",     // Special character ;
            "%s.id:with:colon",        // Special character :
            "%s.id'with'apostrophe",   // Special character '
            "%s.id\"with\"quote",      // Special character "
            "%s.id<with<less>",        // Special character <
            "%s.id>with>greater",      // Special character >
            "%s.id?with?question",     // Special character ?
            "%s.id!with!exclamation",  // Special character !
            "%s.id@with@at",           // Special character @
            "%s.id#with#hashes",       // Special character #
            "%s.id$with$dollar",       // Special character $
            "%s.idwithpercent%%",      // Special character %
            "%s.id^with^caret",        // Special character ^
            "%s.id&with&ampersand",    // Special character &
            "%s.id*with*asterisk",    // Special character *
            "%s.id(with(parentheses)", // Special characters ( )
            "%s.id)with)parentheses)", // Special characters ( )
            "%s.id+with+plus",         // Special character +
            "%s.id=with=equals",       // Special character =
            "%s.id~with~tilde",        // Special character ~
            "%s.id`with`backtick"      // Special character `
        };

        for (String id : invalidIdFragments) {
          DigitalObject digitalObject = TestDigitalObject.generate();
          String objectId = String.format(id, digitalObject.getProject().getProjectAbbr());
          digitalObject.setId(objectId);
          System.out.println("Testing invalid ID: " + objectId);
          org.assertj.core.api.Assertions
              .assertThat(validator.validate(digitalObject).size())
              .withFailMessage(" Supposedly invalid ID is unexpectedly valid: " + objectId)
              .isGreaterThan(0);
        }

      }

    }


      @Test
      public void shouldRaiseNoConstraintViolation() {
        DigitalObject digitalObject = TestDigitalObject.generate();
        Set<ConstraintViolation<DigitalObject>> violationSet = validator.validate(digitalObject);
        assertThat(violationSet, is(empty()));
      }

      @Test
      public void shouldRaiseNoValidationExceptionIfIdIsNull() {
        DigitalObject digitalObject = TestDigitalObject.generate();
        digitalObject.setId(null);
        Set<ConstraintViolation<DigitalObject>> violationSet = validator.validate(digitalObject);
        Assertions.assertThat(violationSet).isNotEmpty();
      }

      @Test
      public void shouldRaiseConstraintViolationIfMetadataIsEmpty() {
        MetadataBaseEntity metadataBaseEntity = new MetadataBaseEntity();
        Set<ConstraintViolation<MetadataBaseEntity>> violationSet = validator.validate(metadataBaseEntity);
        assertThat(violationSet.size(), is(5));
      }

      @Test
      public void shouldNotRaiseConstraintViolationIfMetadataDescriptionIsAnEmptyString() {
        MetadataBaseEntity metadataBaseEntity = TestMetadataBaseEntity.generate();
        // set a description that is too short
        metadataBaseEntity.setDescription("");
        Set<ConstraintViolation<MetadataBaseEntity>> violationSet = validator.validate(metadataBaseEntity);
        assertThat(violationSet.size(), is(0));
      }

      @Test
      public void shouldNotRaiseConstraintViolationIfMetadataDescriptionIsNull() {
        MetadataBaseEntity metadataBaseEntity = TestMetadataBaseEntity.generate();
        // set a description that is too short
        metadataBaseEntity.setDescription(null);
        Set<ConstraintViolation<MetadataBaseEntity>> violationSet = validator.validate(metadataBaseEntity);
        assertThat(violationSet.size(), is(0));
      }

      @Test
      public void shouldRaiseConstraintViolationIfMetadataCreatorIsNull() {
        MetadataBaseEntity metadataBaseEntity = TestMetadataBaseEntity.generate();
        // set creator to null
        metadataBaseEntity.setCreator(null);
        Set<ConstraintViolation<MetadataBaseEntity>> violationSet = validator.validate(metadataBaseEntity);
        assertThat(violationSet.size(), is(1));
      }

      @Test
      public void raisesConstraintViolationIfProjectAbbrIsNotContainedInId() {

        DigitalObject digitalObject = new DigitalObjectBuilder()
            .project(ProjectBuilder.builder().projectAbbr("foo").build())
            .id("foo")
            .publisher("foo")
            .baseMetadata(TestMetadataBaseEntity.generate())
            .build();

        Set<ConstraintViolation<DigitalObject>> violationSet = validator.validate(digitalObject);
        assertThat(violationSet, not(empty()));

      }

      @Test
      public void shouldRaiseOneConstraintViolationIfIdIsTooLong() {
        DigitalObject digitalObject = TestDigitalObject.generate();
        digitalObject.setId(digitalObject.getId() + "12345678901312312321312312323123456789013123123213123123231234567890131231232131231232312345678901312312321312312323"); //
        Set<ConstraintViolation<DigitalObject>> violationSet = validator.validate(digitalObject);
        assertThat(violationSet.size(), is(1));
      }

      @Test
      public void shouldRaiseConstraintViolationIfIdContains$() {
        DigitalObject digitalObject = TestDigitalObject.generate();
        digitalObject.setId(digitalObject.getId() + "$");
        Set<ConstraintViolation<DigitalObject>> violationSet = validator.validate(digitalObject);
        assertThat(violationSet.size(), is(1));
      }

      @Test
      public void shouldRaiseIfIdContainsUppercasedAChar() {
        DigitalObject digitalObject = TestDigitalObject.generate();
        digitalObject.setId(digitalObject.getId() + "A");
        Set<ConstraintViolation<DigitalObject>> violationSet = validator.validate(digitalObject);
        assertThat(violationSet.size(), is(1));
      }
}
