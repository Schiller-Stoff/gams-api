package org.ddh.gamsapi.application.Integration.BaseSearch.Fulltext;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.UnitTest;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrGamsCores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class FulltextQueryBuilderTest extends UnitTest {

  @Nested
  public class BuildSolrUrl {

    @Test
    public void buildsExpectedSolrUrl(){

      String baseQuery = FulltextQueryBuilder.buildBaseSolrQuery(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "some fulltext query"
      );

      List<String> filterQueries = FulltextQueryBuilder.buildSolrFilterQueries(
          MultiValueMap.fromMultiValue(
              Map.of(
                  "dc.title", List.of("Test Title"),
                  "dc.subject", List.of("Subject One", "Subject Two")
              )
          )
      );

      String builtSolrUrl = FulltextQueryBuilder.buildSolrUrl(
          SolrGamsCores.TEST_CORE.value,
          baseQuery,
          filterQueries,
          PageRequest.of(0,10)
      );

      final String EXPECTED_SOLR_BASE_URL = "/solr/test/select?q";
      final String EXPECTED_ENCODED_FULLTEXT_QUERY = "q=objectFulltext:some+fulltext+query%20AND%20objectProjectAbbr:test";
      final String EXPECTED_DC_TITLE_FILTER_QUERY = "dc.title_txt:Test+Title";
      final String EXPECTED_DC_SUBJECT_FILTER_QUERY = "dc.subject_txt:Subject+One";
      final String EXPECTED_FL_URL_PART = "fl=id,objectProjectAbbr,objectDatastreams,objectType,objectTitle,objectDesc,objectCreator,objectPublisher,objectRights,dc.*";

      Assertions.assertThat(builtSolrUrl)
          .contains(
              EXPECTED_SOLR_BASE_URL,
              EXPECTED_ENCODED_FULLTEXT_QUERY,
              EXPECTED_DC_TITLE_FILTER_QUERY,
              EXPECTED_DC_SUBJECT_FILTER_QUERY,
              EXPECTED_FL_URL_PART
          );
    }

  }

}
