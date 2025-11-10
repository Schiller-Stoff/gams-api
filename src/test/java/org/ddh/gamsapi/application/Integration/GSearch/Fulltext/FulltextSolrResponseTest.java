package org.ddh.gamsapi.application.Integration.GSearch.Fulltext;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.UnitTest;
import org.ddh.gamsapi.application.Integration.GSearch.BaseSearchProperties;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class FulltextSolrResponseTest extends UnitTest {

  public final String TEST_SOLR_FULLTEXT_RESPONSE = """
      { 
      "responseHeader":{
              "status":0,
              "QTime":26,
              "params":{
                "hl.tag.pre":"<mark>",
                "hl":"true",
                "indent":"true",
                "fl":"id,objectProjectAbbr,objectDatastreams,objectType,objectTitle,objectDesc,objectCreator,objectPublisher,objectRights,dc.*",
                "hl.requireFieldMatch":"true",
                "start":"0",
                "hl.fragsize":"150",
                "fq":"dc.title:test\\\\-dc\\\\-title",
                "hl.tag.post":"</mark>",
                "rows":"10",
                "hl.simple.pre":"<mark>",
                "hl.snippets":"3",
                "q":"objectFulltext:\\\\* AND objectProjectAbbr:test",
                "hl.simple.post":"</mark>",
                "hl.fl":"objectFulltext",
                "hl.method":"unified",
                "wt":"json"}},
            "response":{"numFound":1,"start":0,"numFoundExact":true,"docs":[
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
                  "objectDesc":["test-description"]}]
            },
            "highlighting":{
              "test.test":{
                "objectFulltext":["...<mark>test-dc-title</mark>...","...<mark>test-dc-title</mark>-en...","...<mark>test-dc-title</mark>..."]
              }}}
      
      """;


  @Nested
  public class From {

    @Test
    public void producesNoNullOrEmptyObject(){
      FulltextSolrResponse fulltextSolrResponse = FulltextSolrResponse.from(TEST_SOLR_FULLTEXT_RESPONSE);
      Assertions.assertThat(fulltextSolrResponse)
          .isNotNull()
          .hasNoNullFieldsOrProperties();
    }

    @Test
    public void producesExpectedObject(){

      FulltextSolrResponse fulltextSolrResponse = FulltextSolrResponse.from(TEST_SOLR_FULLTEXT_RESPONSE);

      Assertions.assertThat(fulltextSolrResponse.getDocuments())
          .hasSize(1);

      Assertions.assertThat(fulltextSolrResponse.getNumFound())
          .isEqualTo(1);

      Assertions.assertThat(fulltextSolrResponse.getHighlighting()
          .get("test.test").get(0))
          .contains("<mark>test-dc-title</mark>");

      Assertions.assertThat(fulltextSolrResponse.getDocuments().get(0).getProperty(BaseSearchProperties.OBJECT_ID.name))
          .isEqualTo(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());

    }



  }

}
