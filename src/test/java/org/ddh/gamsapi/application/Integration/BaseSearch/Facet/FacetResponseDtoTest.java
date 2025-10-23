package org.ddh.gamsapi.application.Integration.BaseSearch.Facet;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.UnitTest;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrDocument;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrFacetedResponse;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;

public class FacetResponseDtoTest extends UnitTest {

  final List<SolrDocument> TEST_SOLR_DOCUMENTS = List.of(new SolrDocument());

  final SolrFacetedResponse TEST_SOLR_RESPONSE = SolrFacetedResponse.builder()
      .facets(new HashMap<>())
      .start(0)
      .totalCount(0)
      .numFound(1)
      .documents(TEST_SOLR_DOCUMENTS)
      .build();

  @Test
  public void fromProducesNotNullObjectWithNoNullProperties() {
    var mapped = FacetResponseDTO.from(TEST_SOLR_RESPONSE, MultiValueMap.fromSingleValue(new HashMap<>()));
    Assertions.assertThat(mapped)
        .isNotNull()
        .hasNoNullFieldsOrProperties();
  }

  @Test
  public void fromProducesCorrectFilteredCount() {
    var mapped = FacetResponseDTO.from(TEST_SOLR_RESPONSE, MultiValueMap.fromSingleValue(new HashMap<>()));
    Assertions.assertThat(mapped.getFilteredCount())
        .isEqualTo(TEST_SOLR_RESPONSE.getNumFound());
  }

  @Test
  public void fromProducesCorrectTotalUnfilteredCount() {
    var mapped = FacetResponseDTO.from(TEST_SOLR_RESPONSE, MultiValueMap.fromSingleValue(new HashMap<>()));
    Assertions.assertThat(mapped.getTotalUnfilteredCount())
        .isEqualTo(TEST_SOLR_RESPONSE.getTotalCount());
  }

  @Test
  public void fromProducesCorrectStart() {
    var mapped = FacetResponseDTO.from(TEST_SOLR_RESPONSE, MultiValueMap.fromSingleValue(new HashMap<>()));
    Assertions.assertThat(mapped.getStart())
        .isEqualTo(TEST_SOLR_RESPONSE.getStart());
  }

  @Test
  public void fromMapsCorrectDocumentsListSize() {
    var mapped = FacetResponseDTO.from(TEST_SOLR_RESPONSE, MultiValueMap.fromSingleValue(new HashMap<>()));
    Assertions.assertThat(mapped.getResults().size())
        .isEqualTo(TEST_SOLR_DOCUMENTS.size());

  }


}
