package org.ddh.gamsapi.application.Integration.BaseSearch.solr;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.UnitTest;
import org.ddh.gamsapi.application.Integration.BaseSearch.Fulltext.FulltextSolrConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class SolrUrlBuilderTest extends UnitTest {

  @Nested
  public class EscapeSolrValue {

    @Test
    public void testEscapeSolrValue() {
      String rawValue = "Special+Characters: (Test)?";
      String escapedValue = SolrUrlBuilder.escapeSolrValue(rawValue);
      String expectedValue = "Special\\+Characters\\: \\(Test\\)\\?";
      Assertions.assertThat(escapedValue).isEqualTo(expectedValue);
    }

  }

  @Nested
  public class UrlEncodeSolrSpecialCharacters {
    @Test
    public void producesExpectedSolrEscapedUrl() {
      final String TEST_SOLR_URL = "http://localhost:55028/solr/gams/select?q=objectFulltext:Gaston AND objectProjectAbbr:test&fq={!tag=type}dc.type:Brief&fq={!tag=coverage}dc.coverage:Paris&start=0&rows=20&fl=id,objectProjectAbbr,objectDatastreams,objectType,objectTitle,objectDesc,objectCreator,objectPublisher,objectRights,dc.*&facet=true&facet.mincount=1&facet.limit=100&facet.sort=count&facet.field={!ex=language}dc.language&facet.field={!ex=coverage}dc.coverage&facet.field={!ex=type}dc.type&facet.field={!ex=creator}dc.creator&facet.field={!ex=subject}dc.subject&facet.field={!ex=format}dc.format&facet.field={!ex=publisher}dc.publisher&wt=json&indent=true";
      final String EXPECTED_ESCAPED_URL = "http://localhost:55028/solr/gams/select?q=objectFulltext:Gaston%20AND%20objectProjectAbbr:test&fq=%7B%21tag=type%7Ddc.type:Brief&fq=%7B%21tag=coverage%7Ddc.coverage:Paris&start=0&rows=20&fl=id,objectProjectAbbr,objectDatastreams,objectType,objectTitle,objectDesc,objectCreator,objectPublisher,objectRights,dc.*&facet=true&facet.mincount=1&facet.limit=100&facet.sort=count&facet.field=%7B%21ex=language%7Ddc.language&facet.field=%7B%21ex=coverage%7Ddc.coverage&facet.field=%7B%21ex=type%7Ddc.type&facet.field=%7B%21ex=creator%7Ddc.creator&facet.field=%7B%21ex=subject%7Ddc.subject&facet.field=%7B%21ex=format%7Ddc.format&facet.field=%7B%21ex=publisher%7Ddc.publisher&wt=json&indent=true";
      String urlEncodedValue = SolrUrlBuilder.urlEncodeSolrSpecialCharacters(TEST_SOLR_URL);
      Assertions.assertThat(urlEncodedValue).isEqualTo(EXPECTED_ESCAPED_URL);
    }
  }

}
