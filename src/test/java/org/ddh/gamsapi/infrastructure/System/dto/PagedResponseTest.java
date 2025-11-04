package org.ddh.gamsapi.infrastructure.System.dto;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.UnitTest;

public class PagedResponseTest extends UnitTest {

  @Nested
  public class FromConversion {

    final Page<DigitalObject> TEST_EMPTY_PAGE = Page.empty();

    @Test
    public void fromGeneratesExpectedEmptyPagedResponse() {
      var converted = PagedResponse.from(TEST_EMPTY_PAGE);
      Assertions.assertThat(converted.getContent()).isEmpty();
      Assertions.assertThat(converted.getPagination().getPage()).isEqualTo(0);
      Assertions.assertThat(converted.getPagination().getSize()).isEqualTo(0);
      Assertions.assertThat(converted.getPagination().getTotalElements()).isEqualTo(0);
      Assertions.assertThat(converted.getMetadata()).isNull();
    }

    @Test
    public void isEmptyReturnsTrueIfEmptyPage() {
      var converted = PagedResponse.from(TEST_EMPTY_PAGE);
      Assertions.assertThat(converted.isEmpty()).isTrue();
    }

    @Test
    public void emptyPageConversionHasSizeOfOne() {
      var converted = PagedResponse.from(TEST_EMPTY_PAGE);
      Assertions.assertThat(converted.getPagination().getTotalPages()).isEqualTo(1);
    }

    @Test
    public void emptyPageConversionHasExpectedPaginationHasResults() {
      var converted = PagedResponse.from(TEST_EMPTY_PAGE);
      Assertions.assertThat(converted.getPagination().isHasNext()).isFalse();
      Assertions.assertThat(converted.getPagination().isHasPrevious()).isFalse();
      Assertions.assertThat(converted.getPagination().isFirst()).isTrue();
      Assertions.assertThat(converted.getPagination().isLast()).isTrue();
    }

  }


}
