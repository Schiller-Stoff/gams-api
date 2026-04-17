package org.ddh.gamsapi.application.Integration.ApiSearch;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestDublinCoreEntry;
import org.ddh.gamsapi.UnitTest;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApiSearchTest extends UnitTest {

  final SolrDocument solrDocument = new SolrDocument();
  final String DC_FIELD = "dc." + TestDublinCoreEntry.NAME.getValue();

  @BeforeEach
  public void setup(){
    solrDocument.addProperty(DC_FIELD, TestDublinCoreEntry.VALUE.getValue());
  }


  @Test
  public void createNotNullOrEmptyPropertyObject(){

    var mappedBasesearch = ApiSearch.from(solrDocument);

    Assertions.assertThat(mappedBasesearch)
        .isNotNull()
        .hasNoNullFieldsOrProperties();
  }

  @Test
  public void createdBaseSearchContainsExpectedValue(){

    var mappedBasesearch = ApiSearch.from(solrDocument);

    Assertions.assertThat(mappedBasesearch.getProperty(DC_FIELD))
        .isNotNull()
        .isEqualTo(TestDublinCoreEntry.VALUE.getValue());
  }

}
