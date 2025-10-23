package org.ddh.gamsapi.application.Integration.BaseSearch.Facet;

import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.TestUtilities.TestBag;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.application.Ingest.Ingest;
import org.ddh.gamsapi.application.Ingest.interfaces.IIngestService;
import org.ddh.gamsapi.application.Ingest.utils.ZipUtils;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearchIntegrationTest;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearchProperties;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearchService;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrClient;
import org.ddh.gamsapi.domain.Project.ProjectBuilder;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import java.io.File;
import java.io.IOException;
import java.util.Set;

@Slf4j
public class FacetServiceIT extends BaseSearchIntegrationTest {

  @Autowired
  private BaseSearchService baseSearchService;

  @Autowired
  private FacetService facetService;

  @Autowired
  private IIngestService ingestService;

  @Autowired
  private IProjectRepository projectRepository;

  // disables auditing
  @MockitoBean
  private AuditingHandler auditingHandler;

  @Autowired
  private SolrClient sOLRClient;

  File bagFile;

  @BeforeEach
  public void setup() throws IOException {
    bagFile = TestBag.loadFile();
    projectRepository.save(ProjectBuilder.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build());

    // ingest the bag
    byte[] zippedBag = ZipUtils.zipDir(bagFile);
    Ingest ingest = new Ingest();
    ingest.setZippedBagItFolder(zippedBag);
    ingest.setProjectAbbr(TestProject.PROJECT_ABBR.getValue());
    ingestService.ingest(ingest);

    // index object
    baseSearchService.indexObject(
        TestProject.PROJECT_ABBR.getValue(), TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
    );
  }

  @Test
  public void facetResultsAreNotEmpty(){

    // Basic search
    var facetResult = facetService.facetSearch(
        Set.of(TestProject.PROJECT_ABBR.getValue()),
        new LinkedMultiValueMap<>(),
        PageRequest.of(0, 20)
    );

    org.assertj.core.api.Assertions.assertThat(facetResult.getResults())
        .isNotEmpty();

    org.assertj.core.api.Assertions.assertThat(facetResult.getAvailableFacets())
        .isNotEmpty();

    org.assertj.core.api.Assertions.assertThat(facetResult.getTotalUnfilteredCount())
        .isGreaterThan(0);

  }

  @Test
  public void facetedResponseContainsExpectedObjectData(){

    // Basic search
    var facetResult = facetService.facetSearch(
        Set.of(TestProject.PROJECT_ABBR.getValue()),
        new LinkedMultiValueMap<>(),
        PageRequest.of(0, 20)
    );

    // TODO rethink assertion
    org.assertj.core.api.Assertions.assertThat(facetResult.getResults())
        .isNotEmpty()
        .anySatisfy( baseSearch -> {
          org.assertj.core.api.Assertions.assertThat(baseSearch.getProperty("id"))
              .isEqualTo(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());
        });

  }

  @Test
  public void facetedResponseDoesNotContainFulltextProperty(){

    // Basic search
    var facetResult = facetService.facetSearch(
        Set.of(TestProject.PROJECT_ABBR.getValue()),
        new LinkedMultiValueMap<>(),
        PageRequest.of(0, 20)
    );

    var returnedBaseSearchElem = facetResult.getResults().get(0);
    org.assertj.core.api.Assertions.assertThat(returnedBaseSearchElem.getProperty(BaseSearchProperties.FULLTEXT.name))
        .isNull();

  }

}
