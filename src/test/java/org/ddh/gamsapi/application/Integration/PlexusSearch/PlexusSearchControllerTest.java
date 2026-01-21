package org.ddh.gamsapi.application.Integration.PlexusSearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.UnitTest;
import org.ddh.gamsapi.application.Integration.PlexusSearch.dto.PlexusSearchQueryRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class PlexusSearchControllerTest extends UnitTest {

  @Nested
  public class RequestDtoDeserializationTest {

    @Test
    @DisplayName("Deserialization with missing fields uses defaults")
    void deserializationWithMissingFieldsUsesDefaults() throws Exception {
      String json = """
            {
                "query": "test"
            }
            """;

      ObjectMapper objectMapper = new ObjectMapper();
      PlexusSearchQueryRequestDto dto = objectMapper.readValue(
          json,
          PlexusSearchQueryRequestDto.class
      );

      // ✅ Verify defaults
      Assertions.assertThat(dto.getQuery()).isEqualTo("test");
      Assertions.assertThat(dto.getRows()).isEqualTo(10);
      Assertions.assertThat(dto.getStart()).isEqualTo(0);
      Assertions.assertThat(dto.getSort()).isEqualTo("id desc");
      Assertions.assertThat(dto.getFilterQueries()).isNotNull().isEmpty();
      Assertions.assertThat(dto.getHighlight()).isFalse();
      Assertions.assertThat(dto.getHighlightSnippetSize()).isEqualTo(200);
      Assertions.assertThat(dto.getFacetLimit()).isEqualTo(10);
      Assertions.assertThat(dto.getFacetMinCount()).isEqualTo(1);
      Assertions.assertThat(dto.getDebug()).isFalse();
    }

    @Test
    @DisplayName("Deserialization with explicit values overrides defaults")
    void deserializationWithExplicitValuesOverridesDefaults() throws Exception {
      String json = """
            {
                "query": "word:test",
                "rows": 50,
                "start": 100,
                "sort": "score desc",
                "highlight": true,
                "filterQueries": ["field:value"]
            }
            """;

      ObjectMapper objectMapper = new ObjectMapper();
      PlexusSearchQueryRequestDto dto = objectMapper.readValue(
          json,
          PlexusSearchQueryRequestDto.class
      );

      // ✅ Explicit values should be used
      Assertions.assertThat(dto.getRows()).isEqualTo(50);
      Assertions.assertThat(dto.getStart()).isEqualTo(100);
      Assertions.assertThat(dto.getSort()).isEqualTo("score desc");
      Assertions.assertThat(dto.getHighlight()).isTrue();
      Assertions.assertThat(dto.getFilterQueries()).containsExactly("field:value");
    }

  }
}
