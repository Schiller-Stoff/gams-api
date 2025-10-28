package org.ddh.gamsapi.application.Integration.BaseSearch.Fulltext;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestBag;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.TestUtilities.TestDublinCoreEntry;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.application.Ingest.Ingest;
import org.ddh.gamsapi.application.Ingest.interfaces.IIngestService;
import org.ddh.gamsapi.application.Ingest.utils.ZipUtils;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearchIntegrationTest;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearchService;
import org.ddh.gamsapi.domain.Project.ProjectBuilder;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Slf4j
public class FulltextServiceIT extends BaseSearchIntegrationTest {

  @Autowired
  private BaseSearchService baseSearchService;

  @Autowired
  private FulltextService fulltextService;

  @Autowired
  private IIngestService ingestService;

  @Autowired
  private IProjectRepository projectRepository;

  // Disables auditing
  @MockitoBean
  private AuditingHandler auditingHandler;

  File bagFile;

  @BeforeEach
  public void setup() throws IOException {
    bagFile = TestBag.loadFile();
    projectRepository.save(ProjectBuilder.builder()
        .projectAbbr(TestProject.PROJECT_ABBR.getValue())
        .build());

    // Ingest the bag
    byte[] zippedBag = ZipUtils.zipDir(bagFile);
    Ingest ingest = new Ingest();
    ingest.setZippedBagItFolder(zippedBag);
    ingest.setProjectAbbr(TestProject.PROJECT_ABBR.getValue());
    ingestService.ingest(ingest);

    // Index object
    baseSearchService.indexObject(
        TestProject.PROJECT_ABBR.getValue(),
        TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
    );
  }

  @Nested
  public class BasicFulltextSearch {

    @Test
    public void findsExpectedTestDublinCoreFulltextValue(){

      var pagedResponse = fulltextService.searchDigitalObjectsByDublinCoreCriteria(
          TestDublinCoreEntry.VALUE.getValue(),
          new HashMap<>(),
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          PageRequest.of(0, 10)
      );

      Assertions.assertThat(pagedResponse.getResults().getContent().size())
          .isEqualTo(1);

      var foundDocument = pagedResponse.getResults().getContent().get(0);

      Assertions.assertThat(foundDocument.getProperty("id"))
          .isEqualTo(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());

    }

    @Test
    public void containsExpectedHighlightingMarksInResults(){

      var pagedResponse = fulltextService.searchDigitalObjectsByDublinCoreCriteria(
          TestDublinCoreEntry.VALUE.getValue(),
          new HashMap<>(),
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          PageRequest.of(0, 10)
      );

      Assertions.assertThat(pagedResponse.getResults().getContent().size())
          .isEqualTo(1);

      var foundDocument = pagedResponse.getResults().getContent().get(0);

      var highlighting = (List<String>) foundDocument.getProperty(FulltextResponseProperties.HIGHLIGHTING.name);

      Assertions.assertThat(highlighting)
          .isNotEmpty();

      Assertions.assertThat(highlighting)
          .hasSize(1);

      var firstSnippet = highlighting.get(0);

      Assertions.assertThat(firstSnippet)
          .contains(FulltextResponseProperties.HIGHLIGHT_PRE.name, FulltextResponseProperties.HIGHLIGHT_POST.name);

    }

    @Test
    public void containsExpectedHighlightingString(){

      var pagedResponse = fulltextService.searchDigitalObjectsByDublinCoreCriteria(
          TestDublinCoreEntry.VALUE.getValue(),
          new HashMap<>(),
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          PageRequest.of(0, 10)
      );

      Assertions.assertThat(pagedResponse.getResults().getContent().size())
          .isEqualTo(1);

      var foundDocument = pagedResponse.getResults().getContent().get(0);

      var highlighting = (List<String>) foundDocument.getProperty(FulltextResponseProperties.HIGHLIGHTING.name);

      Assertions.assertThat(highlighting)
          .isNotEmpty();

      Assertions.assertThat(highlighting)
          .hasSize(1);

      var firstSnippet = highlighting.get(0);

      final String EXPECTED_HIGHLIGHTED_STRING =
          FulltextResponseProperties.HIGHLIGHT_PRE.name +
          TestDublinCoreEntry.VALUE.getValue() +
          FulltextResponseProperties.HIGHLIGHT_POST.name;

      Assertions.assertThat(firstSnippet)
          .contains(EXPECTED_HIGHLIGHTED_STRING);

    }


  }

  @Nested
  public class FulltextSearchWithDcFilters {

    @Test
    public void findsExpectedTestDublinCoreFulltextValueWithDcFilter() {

      HashMap<String, List<String>> dcFilters = new HashMap<>();
      dcFilters.put(
          "dc." + TestDublinCoreEntry.NAME.getValue(),
          List.of(TestDublinCoreEntry.VALUE.getValue())
      );

      var pagedResponse = fulltextService.searchDigitalObjectsByDublinCoreCriteria(
          TestDublinCoreEntry.VALUE.getValue(),
          dcFilters,
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          PageRequest.of(0, 10)
      );

      Assertions.assertThat(pagedResponse.getResults().getContent().size())
          .isEqualTo(1);

      var foundDocument = pagedResponse.getResults().getContent().get(0);

      Assertions.assertThat(foundDocument.getProperty("id"))
          .isEqualTo(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());

    }


  }

}
