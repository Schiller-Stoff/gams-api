package org.ddh.gamsapi.application.Integration.PlexusSearch;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.UnitTest;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrGamsCores;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrUrlBuilder;
import org.ddh.gamsapi.application.Integration.PlexusSearch.dto.PlexusSearchQueryRequestDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

public class PlexusSearchSolrQueryBuilderTest extends UnitTest {

  @Nested
  public class BuildSolrQueryUrl {

    @Test
    public void builtExpectedSimpleUrlContainsExpectedUrlParameterValues() {

      final String EXACT_MATCH_QUERY = String.format("%s:%s",
          PlexusSearchProperties.ENTITY_PROJECT_ABBR.name,
          TestProject.PROJECT_ABBR.getValue()
      );

      PlexusSearchQueryRequestDto requestDto = PlexusSearchQueryRequestDto.builder()
          .query(EXACT_MATCH_QUERY)
          .build();

      var generatedSolrUrl = PlexusSearchSolrQueryBuilder.buildSolrQueryUrl(
          SolrGamsCores.PLEXUS_SEARCH_CORE.value,
          requestDto,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );

      final String expectedContainedQuery = SolrUrlBuilder.urlEncode(EXACT_MATCH_QUERY);
      Assertions.assertThat(generatedSolrUrl)
        .contains(expectedContainedQuery);

      final String expectedContainedProjectAbbrFilter = SolrUrlBuilder.urlEncode(
          String.format("%s:%s",
              PlexusSearchProperties.ENTITY_PROJECT_ABBR.name,
              TestProject.PROJECT_ABBR.getValue()
          )
      );
      Assertions.assertThat(generatedSolrUrl)
          .contains("fq=" + expectedContainedProjectAbbrFilter);


      final String expectedStart = String.format("start=%s", requestDto.getStart());
      Assertions.assertThat(generatedSolrUrl)
          .contains(expectedStart);

      final String expectedRows = String.format("rows=%s", requestDto.getRows());
      Assertions.assertThat(generatedSolrUrl)
          .contains(expectedRows);

      final String expectedCorePath = String.format("/solr/%s/select",
          SolrGamsCores.PLEXUS_SEARCH_CORE.value
      );
      Assertions.assertThat(generatedSolrUrl)
          .contains(expectedCorePath);

      final String expectedSort = String.format("sort=%s", SolrUrlBuilder.urlEncode(requestDto.getSort()));
      Assertions.assertThat(generatedSolrUrl)
          .contains(expectedSort);

    }

    @Test
    public void buildsExpectedComplexUrlContainsExpectedUrlParameterValues() {

      final String EXACT_MATCH_QUERY = String.format("%s:%s",
          PlexusSearchProperties.ENTITY_PROJECT_ABBR.name,
          TestProject.PROJECT_ABBR.getValue()
      );

      PlexusSearchQueryRequestDto requestDto = PlexusSearchQueryRequestDto.builder()
          .query(EXACT_MATCH_QUERY)
          .sort(PlexusSearchProperties.ENTITY_OBJECT_ID.name + " desc")
          .debug(true)
          .facetLimit(50)
          .filterQueries(
              List.of(
                  String.format("%s:[%d TO %d]",
                      PlexusSearchProperties.ENTITY_ID.name,
                      100,
                      200
                  )
              )
          )
          .facetFields(
              List.of(
                  PlexusSearchProperties.ENTITY_PROJECT_ABBR.name,
                  PlexusSearchProperties.ENTITY_ID.name
              )
          )
          .highlight(true)
          .highlightSnippetSize(5)
          .highlightFields(
              List.of(
                  PlexusSearchProperties.ENTITY_OBJECT_ID.name
              )
          )
          .fields(
              List.of(
                  PlexusSearchProperties.ENTITY_ID.name,
                  PlexusSearchProperties.ENTITY_PROJECT_ABBR.name,
                  PlexusSearchProperties.ENTITY_OBJECT_ID.name
              )
          )
          .build();

      var generatedSolrUrl = PlexusSearchSolrQueryBuilder.buildSolrQueryUrl(
          SolrGamsCores.PLEXUS_SEARCH_CORE.value,
          requestDto,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );

      final String EXPECTED_FL_PARAM = String.format("fl=%s",
          String.join(",", requestDto.getFields())
      );
      Assertions.assertThat(generatedSolrUrl)
          .contains(EXPECTED_FL_PARAM);

      final String EXPECTED_FILTER_QUERY_PARAM = SolrUrlBuilder.urlEncode(
          requestDto.getFilterQueries().get(0) // only one in this test case
      );
      Assertions.assertThat(generatedSolrUrl)
          .contains("fq=" + EXPECTED_FILTER_QUERY_PARAM);

      final String EXPECTED_FACET_FIELD_1 = SolrUrlBuilder.urlEncode(
          requestDto.getFacetFields().get(0)
      );
      Assertions.assertThat(generatedSolrUrl)
          .contains("facet.field=" + EXPECTED_FACET_FIELD_1);

      final String EXPECTED_HL_PARAM = String.format("hl=%s",
          requestDto.getHighlight()
      );
      Assertions.assertThat(generatedSolrUrl)
          .contains(EXPECTED_HL_PARAM);

      final String EXPECTED_HL_FIELD = SolrUrlBuilder.urlEncode(
          requestDto.getHighlightFields().get(0) // only one in this test case
      );
      Assertions.assertThat(generatedSolrUrl)
          .contains("hl.fl=" + EXPECTED_HL_FIELD);

      final String EXPECTED_HL_SNIPPET_SIZE = String.format("hl.snippets=%s",
          requestDto.getHighlightSnippetSize()
      );
      Assertions.assertThat(generatedSolrUrl)
          .contains(EXPECTED_HL_SNIPPET_SIZE);

      final String EXPECTED_FACET_LIMIT = String.format("facet.limit=%s",
          requestDto.getFacetLimit()
      );
      Assertions.assertThat(generatedSolrUrl)
          .contains(EXPECTED_FACET_LIMIT);

      final String EXPECTED_SORT = String.format("sort=%s",
          SolrUrlBuilder.urlEncode(requestDto.getSort())
      );
      Assertions.assertThat(generatedSolrUrl)
          .contains(EXPECTED_SORT);


    }


  }


}
