package org.ddh.gamsapi.application.Integration.PlexusSearch;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.UnitTest;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrGamsCores;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrUrlBuilder;
import org.ddh.gamsapi.application.Integration.PlexusSearch.dto.PlexusSearchQueryRequestDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

      System.out.println(generatedSolrUrl);

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
          .contains(expectedContainedProjectAbbrFilter);


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


  }


}
