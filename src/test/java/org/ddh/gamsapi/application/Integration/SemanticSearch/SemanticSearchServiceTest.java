package org.ddh.gamsapi.application.Integration.SemanticSearch;

import org.ddh.gamsapi.application.Integration.SemanticSearch.exceptions.SemanticSearchIOException;
import org.ddh.gamsapi.application.Integration.SemanticSearch.exceptions.SemanticSearchNoTriplesExtractableException;
import org.ddh.gamsapi.application.Integration.SemanticSearch.utils.QLeverBulkExporter;
import org.ddh.gamsapi.application.Integration.SemanticSearch.utils.QleverClient;
import org.ddh.gamsapi.domain.Datastream.DatastreamId;
import org.ddh.gamsapi.domain.Datastream.utils.exceptions.DatastreamNotFoundException;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamIndexingView;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = SemanticSearchService.class)
class SemanticSearchServiceTest {

  @Autowired
  private SemanticSearchService semanticSearchService;

  @MockitoBean
  private QleverClient qleverClient;

  @MockitoBean
  private QLeverBulkExporter bulkExporter;

  @MockitoBean
  private IDigitalObjectRepository digitalObjectRepository;

  @MockitoBean
  private IDatastreamRepository datastreamRepository;

  @MockitoBean
  private IDatastreamContentRepository datastreamContentRepository;

  private static final String PROJECT_ABBR = "TEST_PROJ";
  private static final String OBJECT_ID = "o:test.1";
  private static final String GRAPH_URI = "https://gams.uni-graz.at/pub/TEST_PROJ";

  @Test
  @DisplayName("indexObject: Should successfully extract triples and send to QLever")
  void indexObject_ValidObject_Success() throws IOException {
    // 1. Arrange
    DatastreamId expectedDsid = DatastreamId.builder()
        .dsid(SemanticSearchProperties.DATASTREAM_DSID.name)
        .digitalObject(OBJECT_ID)
        .build();

    String validTurtle = "@prefix ex: <https://example.org/> .\n ex:subject ex:predicate ex:object .";
    ByteArrayInputStream inputStream = new ByteArrayInputStream(validTurtle.getBytes());
    InputStreamResource mockResource = new InputStreamResource(inputStream);

    when(digitalObjectRepository.existsById(OBJECT_ID)).thenReturn(true);
    when(datastreamContentRepository.exists(expectedDsid)).thenReturn(true);
    when(datastreamContentRepository.findById(expectedDsid))
        .thenReturn(mockResource);

    // 2. Act
    semanticSearchService.indexObject(PROJECT_ABBR, OBJECT_ID);

    // 3. Assert
    // Verify triples were extracted and sent to QLever
    verify(qleverClient, times(1)).insertDataIntoGraph(
        eq(GRAPH_URI),
        anySet(),
        contains("ex:subject ex:predicate ex:object ."),
        anyString()
    );
    // Verify index rebuild was triggered
    verify(qleverClient, times(1)).rebuildIndex();
  }

  @Test
  @DisplayName("indexObject: Should throw exception if Digital Object doesn't exist")
  void indexObject_ObjectNotFound_ThrowsException() {
    when(digitalObjectRepository.existsById(OBJECT_ID)).thenReturn(false);

    assertThrows(DigitalObjectNotFoundException.class, () ->
        semanticSearchService.indexObject(PROJECT_ABBR, OBJECT_ID)
    );

    verifyNoInteractions(qleverClient);
  }

  @Test
  @DisplayName("indexObject: Should throw exception if TTL datastream is missing")
  void indexObject_DatastreamNotFound_ThrowsException() {
    when(digitalObjectRepository.existsById(OBJECT_ID)).thenReturn(true);
    when(datastreamContentRepository.exists(any(DatastreamId.class))).thenReturn(false);

    assertThrows(DatastreamNotFoundException.class, () ->
        semanticSearchService.indexObject(PROJECT_ABBR, OBJECT_ID)
    );

    verifyNoInteractions(qleverClient);
  }

  @Test
  @DisplayName("indexObject: Should throw exception if TTL contains no extractable triples")
  void indexObject_NoTriples_ThrowsException() {
    DatastreamId expectedDsid = DatastreamId.builder()
        .dsid(SemanticSearchProperties.DATASTREAM_DSID.name)
        .digitalObject(OBJECT_ID)
        .build();

    // Only prefixes, no actual statements
    String invalidTurtle = "@prefix ex: <https://example.org/> .";
    // 1. Create a ByteArrayInputStream from your string
    ByteArrayInputStream inputStream = new ByteArrayInputStream(invalidTurtle.getBytes());

    // 2. Wrap it in the specific InputStreamResource that the repository returns
    InputStreamResource mockResource = new InputStreamResource(inputStream);

    when(digitalObjectRepository.existsById(OBJECT_ID)).thenReturn(true);
    when(datastreamContentRepository.exists(expectedDsid)).thenReturn(true);
    when(datastreamContentRepository.findById(expectedDsid))
        .thenReturn(mockResource);

    assertThrows(SemanticSearchNoTriplesExtractableException.class, () ->
        semanticSearchService.indexObject(PROJECT_ABBR, OBJECT_ID)
    );

    verifyNoInteractions(qleverClient);
  }

  @Test
  @DisplayName("indexObjects (Batch): Should paginate through datastreams and flush batches")
  void indexObjects_ProjectBatch_Success() throws IOException {
    // 1. Arrange
    IDatastreamIndexingView mockView = createMockDatastreamView(OBJECT_ID);

    // Return 1 page with 1 item, then an empty page to stop the loop
    when(datastreamRepository.findAllByDsidAndProject(
        eq(SemanticSearchProperties.DATASTREAM_DSID.name),
        eq(PROJECT_ABBR),
        any(Pageable.class)
    )).thenReturn(new PageImpl<>(List.of(mockView)))
        .thenReturn(Page.empty());

    DatastreamId expectedDsid = DatastreamId.builder()
        .dsid(SemanticSearchProperties.DATASTREAM_DSID.name)
        .digitalObject(OBJECT_ID)
        .build();

    String validTurtle = "@prefix ex: <https://example.org/> .\n ex:subject ex:predicate ex:object .";
    // 1. Create a ByteArrayInputStream from your string
    ByteArrayInputStream inputStream = new ByteArrayInputStream(validTurtle.getBytes());

    // 2. Wrap it in the specific InputStreamResource that the repository returns
    InputStreamResource mockResource = new InputStreamResource(inputStream);

    when(datastreamContentRepository.findById(expectedDsid))
        .thenReturn(mockResource);

    // 2. Act
    semanticSearchService.indexObjects(PROJECT_ABBR);

    // 3. Assert
    // Verify graph was dropped prior to batch insert
    verify(qleverClient, times(1)).dropGraph(GRAPH_URI, PROJECT_ABBR);

    // Verify data was inserted
    verify(qleverClient, times(1)).insertDataIntoGraph(
        eq(GRAPH_URI),
        anySet(),
        anyString(),
        anyString()
    );

    // Verify index rebuild was triggered exactly once at the end
    verify(qleverClient, times(1)).rebuildIndex();
  }

  @Test
  @DisplayName("deleteIndexedObjects: Translates IO exceptions into domain exceptions")
  void deleteIndexedObjects_QleverFails_ThrowsDomainException() throws IOException {
    doThrow(new IOException("QLever is down")).when(qleverClient).dropGraph(GRAPH_URI, PROJECT_ABBR);

    SemanticSearchIOException exception = assertThrows(SemanticSearchIOException.class, () ->
        semanticSearchService.deleteIndexedObjects(PROJECT_ABBR)
    );

    assert(exception.getMessage().contains("Failed to drop graph"));
  }

  // --- Helper Methods ---

  private IDatastreamIndexingView createMockDatastreamView(String objectId) {
    return new IDatastreamIndexingView() {
      @Override
      public String getDsid() {
        return SemanticSearchProperties.DATASTREAM_DSID.name;
      }
      @Override
      public DigitalObjectIdView getDigitalObject() {
        return () -> objectId;
      }
    };
  }
}