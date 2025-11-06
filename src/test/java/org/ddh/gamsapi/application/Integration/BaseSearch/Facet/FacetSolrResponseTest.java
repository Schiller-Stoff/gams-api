package org.ddh.gamsapi.application.Integration.BaseSearch.Facet;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.UnitTest;
import org.junit.jupiter.api.Test;

public class FacetSolrResponseTest extends UnitTest {

  final String NUM_FOUND = "1";
  final String START = "0";

  final String TEST_SOLR_RESPONSE = String.format("""
      {
        "responseHeader":{
          "status":0,
          "QTime":18,
          "params":{
            "q":"objectProjectAbbr:test AND dc.title:\\"test\\\\-dc\\\\-title\\"",
            "facet.limit":"100",
            "facet.field":["dc.subject",
              "dc.format",
              "dc.publisher",
              "dc.language",
              "dc.coverage",
              "dc.type",
              "dc.creator"],
            "indent":"true",
            "start":"0",
            "facet.mincount":"1",
            "rows":"20",
            "facet":"true",
            "wt":"json",
            "facet.sort":"count"}},
        "response":{"numFound":%s,"start":%s,"numFoundExact":true,"docs":[
            {
              "dc.publisher":["Institut für Sprachwissenschaft, Karl-Franzens-Universität Graz",
                "en:test-dc-publisher-en"],
              "dc.format":["text/xml"],
              "dc.rights":["Creative Commons BY-NC 4.0",
                "http://creativecommons.org/licenses/by-nc/4.0"],
              "objectType":"digitalObject",
              "dc.type":["Brief"],
              "dc.source":["Universitätsbibliothek Graz Abteilung für Sondersammlungen 08564"],
              "dc.creator":["Paris, Gaston",
                "en:test-dc-creator-en"],
              "objectFulltext":"test-dc-title test-dc-title-en test-dc-title test-dc-title-en test-dc-description test-dc-description-en Hugo Schuchardt Archiv http://schuchardt.uni-graz.at test-dc-relation-en Paris, Gaston test-dc-creator-en Institut für Sprachwissenschaft, Karl-Franzens-Universität Graz test-dc-publisher-en Schuchardt, Hugo test-dc-contributor-en fr test-dc-language-en 1873-02-17 test-dc-date-en Brief text/xml Universitätsbibliothek Graz Abteilung für Sondersammlungen 08564 Paris Creative Commons BY-NC 4.0 http://creativecommons.org/licenses/by-nc/4.0 Romania (Zeitschrift) Rumänisch",
              "id":"test.test",
              "dc.description":["test-dc-description",
                "en:test-dc-description-en"],
              "objectTitle":["test-title"],
              "dc.coverage":["Paris"],
              "dc.date":["1873-02-17",
                "en:test-dc-date-en"],
              "objectPublisher":["test-publisher"],
              "objectCreator":["test-creator"],
              "dc.contributor":["Schuchardt, Hugo",
                "en:test-dc-contributor-en"],
              "objectProjectAbbr":"test",
              "objectDatastreams":["DC.xml",
                "manifest.json",
                "search.json",
                "test.txt",
                "test.xml"],
              "dc.relation":["Hugo Schuchardt Archiv",
                "http://schuchardt.uni-graz.at",
                "en:test-dc-relation-en"],
              "dc.title":["test-dc-title",
                "en:test-dc-title-en"],
              "dc.identifier":["test-dc-title",
                "en:test-dc-title-en"],
              "dc.subject":["\\n        Romania (Zeitschrift)\\n    ",
                "\\n        Rumänisch\\n    "],
              "dc.language":["fr",
                "en:test-dc-language-en"],
              "objectRights":["test-rights"],
              "objectDesc":["test-description"],
              "_version_":1846760176301899776}]
        },
        "facet_counts":{
          "facet_queries":{},
          "facet_fields":{
            "dc.subject":[
              "\\n        Romania (Zeitschrift)\\n    ",1,
              "\\n        Rumänisch\\n    ",1],
            "dc.format":[
              "text/xml",1],
            "dc.publisher":[
              "Institut für Sprachwissenschaft, Karl-Franzens-Universität Graz",1,
              "en:test-dc-publisher-en",1],
            "dc.language":[
              "en:test-dc-language-en",1,
              "fr",1],
            "dc.coverage":[
              "Paris",1],
            "dc.type":[
              "Brief",1],
            "dc.creator":[
              "Paris, Gaston",1,
              "en:test-dc-creator-en",1]},
          "facet_ranges":{},
          "facet_intervals":{},
          "facet_heatmaps":{}}}
      """,
      NUM_FOUND,
      START
  );


  @Test
  public void parsedSolrFacetedResponseIsNotNull() {
    var parsedResponse = FacetSolrResponse.from(TEST_SOLR_RESPONSE);

    Assertions.assertThat(parsedResponse)
        .isNotNull()
        .hasNoNullFieldsOrProperties();

  }

  @Test
  public void containsExpectedNumFound() {
    var parsedResponse = FacetSolrResponse.from(TEST_SOLR_RESPONSE);

    Assertions.assertThat(parsedResponse.getNumFound())
        .isEqualTo(Long.parseLong(NUM_FOUND));
  }

  @Test
  public void containsExpectedStart() {
    var parsedResponse = FacetSolrResponse.from(TEST_SOLR_RESPONSE);

    Assertions.assertThat(parsedResponse.getStart())
        .isEqualTo(Long.parseLong(START));
  }

  @Test
  public void containsExpectedFacets() {
    var parsedResponse = FacetSolrResponse.from(TEST_SOLR_RESPONSE);

    Assertions.assertThat(parsedResponse.getFacets())
        .isNotEmpty()
        .containsKeys(
            "dc.subject",
            "dc.format",
            "dc.publisher",
            "dc.language",
            "dc.coverage",
            "dc.type",
            "dc.creator"
        );
  }


}
