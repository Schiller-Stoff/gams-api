package org.ddh.gamsapi.application.Integration.BaseSearch.Facet;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.MultiValueMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FacetQueryBuilderTest extends UnitTest {

  @Nested
  public class BuildSolrFacetUrl {

    @Test
    public void builtSolrUrlContainsExpectedValues(){
      var TEST_CORE_NAME = "test_core";
      var TEST_FACET_QUERY = "dc.title:Test";
      var TEST_FACET_FIELDS = Set.of("dc.title", "dc.creator");
      var TEST_PAGEABLE =  PageRequest.of(
          0,
          10,
          org.springframework.data.domain.Sort.by("dc.title").ascending()
      );

      var builtUrl =  FacetQueryBuilder.buildSolrFacetUrl(TEST_CORE_NAME, TEST_FACET_QUERY, TEST_FACET_FIELDS, TEST_PAGEABLE);

      Assertions.assertThat(builtUrl)
          .isNotEmpty()
          .contains("/solr/test_core/select")
          .contains("q=dc.title:Test")
          .contains("start=0")
          .contains("rows=10")
          .contains("sort=dc.title asc")
          .contains("fl=")
          .contains("dc.title")
          .contains("dc.creator");
    }

  }


  @Nested
  public class BuildSolrFacetQuery {

    MultiValueMap<String, String> TEST_DC_MAP;

    @BeforeEach
    public void setup(){
      final Map<String, List<String>> TEST_MAP = new HashMap<String, List<String>>();
      TEST_MAP.put("dc.title", List.of("Title 1", "Title 2"));
      TEST_MAP.put("dc.creator", List.of("Creator A"));
      TEST_DC_MAP = MultiValueMap.fromMultiValue(TEST_MAP);
    }

    @Test
    public void builtSolrQueryContainsExpectedValues(){
      var TEST_PROJECTS = Set.of(TestProject.PROJECT_ABBR.getValue());

      var builtQuery =  FacetQueryBuilder.buildSolrFacetQuery(TEST_PROJECTS,"", TEST_DC_MAP);

      Assertions.assertThat(builtQuery)
          .isNotEmpty()
          .contains("dc.title")
          .contains("dc.creator")
          .contains(TestProject.PROJECT_ABBR.getValue());



    }


  }


}
