package org.ddh.gamsapi.application.Integration.PlexusSearch.validator;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.UnitTest;
import org.ddh.gamsapi.application.Integration.PlexusSearch.PlexusSearchProperties;
import org.ddh.gamsapi.application.Integration.PlexusSearch.dto.PlexusSearchQueryRequestDto;
import org.ddh.gamsapi.application.Integration.PlexusSearch.exceptions.PlexusSearchForbiddenQueryException;
import org.ddh.gamsapi.application.Integration.PlexusSearch.validation.PlexusSearchQueryValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

public class PlexusSearchQueryValidatorTest extends UnitTest {

  PlexusSearchQueryValidator plexusSearchQueryValidator = new PlexusSearchQueryValidator();

  PlexusSearchQueryRequestDto VALID_TEST_REQUEST;

  @BeforeEach
  public void setup(){
    VALID_TEST_REQUEST = PlexusSearchQueryRequestDto.builder()
        .query(
            String.format("%s:%s", PlexusSearchProperties.ENTITY_PROJECT_ABBR.name, TestProject.PROJECT_ABBR.getValue())
        )
        .start(0)
        .rows(10)
        .sort("id desc")
        .filterQueries(
            List.of(
                String.format("%s:%s",
                    PlexusSearchProperties.ENTITY_PROJECT_ABBR.name,
                    TestProject.PROJECT_ABBR.getValue()
                )
            )
        )
        .fields(List.of(PlexusSearchProperties.ENTITY_ID.name))
        .highlight(true)
        .highlightFields(List.of(PlexusSearchProperties.ENTITY_OBJECT_ID.name))
        .highlightSnippetSize(150)
        .facetFields(List.of(PlexusSearchProperties.ENTITY_PROJECT_ABBR.name))
        .facetLimit(5)
        .facetMinCount(1)
        .build();
  }

  @Nested
  public class ValidateQuery {

    @Test
    public void shouldNotThrowAtValidPlexusSearchQueryDto() {
      Assertions.assertThatNoException().isThrownBy(() ->
          plexusSearchQueryValidator.validateQuery(
              VALID_TEST_REQUEST,
              TestProject.PROJECT_ABBR.getValue()
          )
      );
    }

    @Test
    public void throwsIfQueryIsEmpty(){
      VALID_TEST_REQUEST.setQuery("");
      Assertions.assertThatThrownBy(() ->
          plexusSearchQueryValidator.validateQuery(
              VALID_TEST_REQUEST,
              TestProject.PROJECT_ABBR.getValue()
          )
      ).isInstanceOf(PlexusSearchForbiddenQueryException.class);
    }

    @Test
    public void throwsIfQueryIsTooLong(){
      VALID_TEST_REQUEST.setQuery("id:".repeat(5001));
      Assertions.assertThatThrownBy(() ->
          plexusSearchQueryValidator.validateQuery(
              VALID_TEST_REQUEST,
              TestProject.PROJECT_ABBR.getValue()
          )
      ).isInstanceOf(PlexusSearchForbiddenQueryException.class);
    }

    @Test
    public void throwsIfForbiddenPatternsAreInQuery(){

      VALID_TEST_REQUEST.setQuery("id:javascript:");
      Assertions.assertThatThrownBy(() ->
          plexusSearchQueryValidator.validateQuery(
              VALID_TEST_REQUEST,
              TestProject.PROJECT_ABBR.getValue()
          )
      ).isInstanceOf(PlexusSearchForbiddenQueryException.class);
    }

    @Test
    public void throwsIfQueryStartsWithWildcard(){
      VALID_TEST_REQUEST.setQuery("*:*");
      Assertions.assertThatThrownBy(() ->
          plexusSearchQueryValidator.validateQuery(
              VALID_TEST_REQUEST,
              TestProject.PROJECT_ABBR.getValue()
          )
      ).isInstanceOf(PlexusSearchForbiddenQueryException.class);
    }
  }

  @Nested
  public class ValidatePagination {

    @Test
    public void throwsNotIfStartIsNull(){
      VALID_TEST_REQUEST.setStart(null);
      Assertions.assertThatNoException().isThrownBy(() ->
          plexusSearchQueryValidator.validateQuery(VALID_TEST_REQUEST, TestProject.PROJECT_ABBR.getValue())
      );
    }

    @Test
    public void throwsIfStartIsNegative() {
      VALID_TEST_REQUEST.setStart(-1);
      Assertions.assertThatThrownBy(() ->
          plexusSearchQueryValidator.validateQuery(VALID_TEST_REQUEST, TestProject.PROJECT_ABBR.getValue())
      ).isInstanceOf(PlexusSearchForbiddenQueryException.class);
    }


    @Test
    public void throwsNotIfRowsIsNull() {
      VALID_TEST_REQUEST.setRows(null);
      Assertions.assertThatNoException().isThrownBy(() ->
          plexusSearchQueryValidator.validateQuery(VALID_TEST_REQUEST, TestProject.PROJECT_ABBR.getValue())
      );
    }

    @Test
    public void throwsIfRowsIsNegative() {
      VALID_TEST_REQUEST.setRows(-5);
      Assertions.assertThatThrownBy(() ->
          plexusSearchQueryValidator.validateQuery(VALID_TEST_REQUEST, TestProject.PROJECT_ABBR.getValue())
      ).isInstanceOf(PlexusSearchForbiddenQueryException.class);
    }

    @Test
    public void throwsIfRowsExceedsMaximum() {
      VALID_TEST_REQUEST.setRows(10001);
      Assertions.assertThatThrownBy(() ->
          plexusSearchQueryValidator.validateQuery(VALID_TEST_REQUEST, TestProject.PROJECT_ABBR.getValue())
      ).isInstanceOf(PlexusSearchForbiddenQueryException.class);
    }
  }



}
