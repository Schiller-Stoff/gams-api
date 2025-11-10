package org.ddh.gamsapi.application.Integration.CustomSearch;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.UnitTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

public class CustomSearchSolrQueryBuilderTest extends UnitTest {

  @Nested
  public class BuildFilterQueries {

    @Test
    public void buildsExpectedFilterQueries(){

      final List<String> TAG_FILTERS = List.of("tag1", "tag2", "tag3");

      var filterQueries = CustomSearchSolrQueryBuilder.buildFilterQueries(TAG_FILTERS);

      Assertions.assertThat(
          filterQueries
      )
          .isNotEmpty()
          .hasSize(1);

      Assertions.assertThat(
          filterQueries.get(0)
      )
          .isEqualTo("(entityTags:tag1 AND entityTags:tag2 AND entityTags:tag3)");

    }


  }

  @Nested
  public class BuildDateFilterQueries {

    @Test
    void buildDateFilterQueries_withBothDates() {
      List<String> filters = CustomSearchSolrQueryBuilder.buildDateFilterQueries(
          "2023-01-01T00:00:00Z",
          "2023-12-31T23:59:59Z"
      );
      Assertions.assertThat(filters).hasSize(2);
      Assertions.assertThat(filters.get(0)).contains("entityEndDate:[2023-01-01T00:00:00Z TO *]");
      Assertions.assertThat(filters.get(1)).contains("entityStartDate:[* TO 2023-12-31T23:59:59Z]");
    }

    @Test
    void buildDateFilterQueries_withOnlyStartDate() {
      List<String> filters = CustomSearchSolrQueryBuilder.buildDateFilterQueries(
          "2023-01-01T00:00:00Z", null
      );
      Assertions.assertThat(filters).hasSize(1);
    }


  }


}
