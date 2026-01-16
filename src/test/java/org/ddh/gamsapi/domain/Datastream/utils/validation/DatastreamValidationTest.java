package org.ddh.gamsapi.domain.Datastream.utils.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.ddh.gamsapi.TestUtilities.TestDatastream;
import org.ddh.gamsapi.UnitTest;
import org.ddh.gamsapi.domain.Datastream.Datastream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

public class DatastreamValidationTest extends UnitTest {

  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  @BeforeAll
  static void init() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @Nested
  public class DsidValidation {

    @Test
    public void createsNoConstraintViolationsForListOfValidDatastreams() {

      var VALID_DATASTREAMS = List.of(
          // Standard GAMS datastreams
          "TEI_SOURCE.xml",
          "DC.xml",
          "THUMBNAIL.jpg",
          "STYLESHEET.xsl",
          "CONTEXT.xml",
          "MANIFEST.json",
          "CUSTOM_SEARCH.json",

          // missing file extensions
          "TEI_SOURCE",
          "DC",
          "THUMBNAIL",

          // Underscores in middle/end (OK)
          "test_file.xml",
          "dc_metadata.xml",
          "TEI_SOURCE.xml",
          "SOME_RANDOM_DSID.txt",

          // Hyphens anywhere after first character
          "TEI-SOURCE.xml",
          "document-v1.xml",
          "thumbnail-large.jpg",
          "a-b-c.xml",

          // Numbers at start
          "1TEI.xml",
          "123_document.xml",
          "001-file.xml",

          // Mixed valid combinations
          "TEI_SOURCE-v1.xml",
          "document_2024-final.xml",
          "1test_file-backup.xml",

          // Multiple dots (extensions)
          "archive.tar.gz",
          "file.backup.xml",
          "document.v1.xml",

          // Single character base
          "a.xml",
          "1.xml",
          "T.jpg",

          // additional examples
          "manifest.json",
          "search.json",
          "test.xml",
          "test.txt"
      );

      for (String dsid : VALID_DATASTREAMS) {
        Datastream datastream = TestDatastream.generate();
        // format the id with the project's abbreviation from the test digital object
        datastream.setDsid(dsid);
        org.assertj.core.api.Assertions.assertThat(
                validator.validate(datastream))
            .isEmpty();
      }

    }

    @Test
    public void invalidDatastreamShouldCreateExepectedViolationsCount() {
      var INVALID_DATASTREAMS = List.of(
          // Starts with underscore (NOW FORBIDDEN)
          "_test.xml",
          "_private.xml",
          "_hidden_file.xml",

          // Starts with hyphen
          "-document.xml",
          "-test.xml",

          // Starts with dot
          ".hidden.xml",
          ".config.xml",

          // Consecutive dots
          "document..xml",
          "file...xml",
          "test..backup.xml",

          // Consecutive hyphens
          "document--v1.xml",
          "file---backup.xml",

          // Consecutive underscores
          "test__file.xml",
          "data___set.xml",

          // Special characters
          "test!.xml",
          "doc#1.xml",
          "file$.xml",
          "test test.xml",  // space
          "doc@file.xml",
          "file*name.xml",
          "doc&file.xml",

          // empty value
          "",

          // Paths
          "../document.xml",
          "./file.xml",
          "folder/file.xml"
      );


      int violationCount = 0;
      for (String dsid : INVALID_DATASTREAMS) {
        Datastream datastream = TestDatastream.generate();
        // format the id with the project's abbreviation from the test digital object
        datastream.setDsid(dsid);
        org.assertj.core.api.Assertions.assertThat(
                validator.validate(datastream))
            .isNotEmpty();
        violationCount++;
      }
      org.assertj.core.api.Assertions.assertThat(violationCount)
          .isEqualTo(INVALID_DATASTREAMS.size());
    }

  }

}
