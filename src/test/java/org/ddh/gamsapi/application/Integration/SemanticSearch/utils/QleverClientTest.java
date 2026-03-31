package org.ddh.gamsapi.application.Integration.SemanticSearch.utils;

import org.ddh.gamsapi.infrastructure.System.configproperties.GAMSDockerDNS;
import org.ddh.gamsapi.infrastructure.System.configproperties.SemanticSearchProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QleverClientTest {

    @Mock(strictness = Mock.Strictness.LENIENT)
    private GAMSDockerDNS gamsDockerDNS;
    @Mock
    private SemanticSearchProperties semanticSearchProperties;
    @Mock
    private RestTemplate restTemplate;

    private QleverClient qleverClient;

    private final String QLEVER_URL = "http://qlever:7001";
    private final String ACCESS_TOKEN = "test-access-token";

    @BeforeEach
    void setUp() {
        when(gamsDockerDNS.getQleverUrl()).thenReturn(QLEVER_URL);
        when(semanticSearchProperties.getAccessToken()).thenReturn(ACCESS_TOKEN);

        qleverClient = spy(new QleverClient(gamsDockerDNS, semanticSearchProperties, restTemplate));
    }

    @Test
    void postSparqlUpdate_success() throws IOException {
        String sparql = "INSERT DATA { GRAPH <http://example.org/graph> { <http://example.org/s> <http://example.org/p> <http://example.org/o> . } }";
        String context = "test-context";
        String expectedUrl = QLEVER_URL + "?access-token=" + ACCESS_TOKEN;

        ResponseEntity<String> successResponse = new ResponseEntity<>("OK", HttpStatus.OK);
        when(restTemplate.postForEntity(eq(expectedUrl), any(HttpEntity.class), eq(String.class)))
                .thenReturn(successResponse);

        qleverClient.postSparqlUpdate(sparql, context);

        ArgumentCaptor<HttpEntity<String>> httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(expectedUrl), httpEntityCaptor.capture(), eq(String.class));

        HttpEntity<String> capturedEntity = httpEntityCaptor.getValue();
        assertEquals(sparql, capturedEntity.getBody());
        assertEquals(MediaType.valueOf("application/sparql-update; charset=utf-8"), capturedEntity.getHeaders().getContentType());
    }

    @Test
    void postSparqlUpdate_qleverReturnsError() {
        String sparql = "INVALID SPARQL";
        String context = "test-context";
        String expectedUrl = QLEVER_URL + "?access-token=" + ACCESS_TOKEN;

        ResponseEntity<String> errorResponse = new ResponseEntity<>("Error from QLever", HttpStatus.BAD_REQUEST);
        when(restTemplate.postForEntity(eq(expectedUrl), any(HttpEntity.class), eq(String.class)))
                .thenReturn(errorResponse);

        IOException thrown = assertThrows(IOException.class, () -> qleverClient.postSparqlUpdate(sparql, context));
        assertTrue(thrown.getMessage().contains("QLever returned error for SPARQL Update"));
        assertTrue(thrown.getMessage().contains("Status: 400 BAD_REQUEST"));
        assertTrue(thrown.getMessage().contains("Body: Error from QLever"));
    }

    @Test
    void postSparqlUpdate_restClientException() {
        String sparql = "SOME SPARQL";
        String context = "test-context";
        String expectedUrl = QLEVER_URL + "?access-token=" + ACCESS_TOKEN;

        when(restTemplate.postForEntity(eq(expectedUrl), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        IOException thrown = assertThrows(IOException.class, () -> qleverClient.postSparqlUpdate(sparql, context));
        assertTrue(thrown.getMessage().contains("Failed to send SPARQL Update to QLever"));
        assertTrue(thrown.getMessage().contains("Cause: Connection refused"));
        assertNotNull(thrown.getCause());
        assertTrue(thrown.getCause() instanceof RestClientException);
    }

    @Test
    void insertDataIntoGraph_success() throws IOException {
        String graphUri = "http://example.org/graph";
        Set<String> prefixes = Set.of("PREFIX ex: <http://example.org/>", "PREFIX foo: <http://foo.bar/>");
        String triples = "ex:s ex:p ex:o .";
        String context = "insert-context";

        // Mock postSparqlUpdate to do nothing, as we only care about the generated SPARQL
        doNothing().when(qleverClient).postSparqlUpdate(anyString(), anyString());

        qleverClient.insertDataIntoGraph(graphUri, prefixes, triples, context);

        ArgumentCaptor<String> sparqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(qleverClient).postSparqlUpdate(sparqlCaptor.capture(), eq(context));

        String capturedSparql = sparqlCaptor.getValue();
        assertTrue(capturedSparql.contains("PREFIX ex: <http://example.org/>"));
        assertTrue(capturedSparql.contains("PREFIX foo: <http://foo.bar/>"));
        assertTrue(capturedSparql.contains("INSERT DATA { GRAPH <http://example.org/graph> {"));
        assertTrue(capturedSparql.contains("ex:s ex:p ex:o ."));
        assertTrue(capturedSparql.contains("} }"));
    }

    @Test
    void dropGraph_success() throws IOException {
        String graphUri = "http://example.org/graph";
        String context = "drop-context";

        doNothing().when(qleverClient).postSparqlUpdate(anyString(), anyString());

        qleverClient.dropGraph(graphUri, context);

        ArgumentCaptor<String> sparqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(qleverClient).postSparqlUpdate(sparqlCaptor.capture(), eq(context));

        String capturedSparql = sparqlCaptor.getValue();
        assertEquals("DROP SILENT GRAPH <http://example.org/graph>", capturedSparql);
    }

    @Test
    void clearGraph_success() throws IOException {
        String graphUri = "http://example.org/graph";
        String context = "clear-context";

        doNothing().when(qleverClient).postSparqlUpdate(anyString(), anyString());

        qleverClient.clearGraph(graphUri, context);

        ArgumentCaptor<String> sparqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(qleverClient).postSparqlUpdate(sparqlCaptor.capture(), eq(context));

        String capturedSparql = sparqlCaptor.getValue();
        assertEquals("CLEAR SILENT GRAPH <http://example.org/graph>", capturedSparql);
    }

    @Test
    void rebuildIndex_success() throws IOException {
        String expectedUrl = QLEVER_URL + "?cmd=rebuild-index&access-token=" + ACCESS_TOKEN;

        ResponseEntity<String> successResponse = new ResponseEntity<>("Index rebuilt", HttpStatus.OK);
        when(restTemplate.getForEntity(eq(expectedUrl), eq(String.class)))
                .thenReturn(successResponse);

        qleverClient.rebuildIndex();

        verify(restTemplate).getForEntity(eq(expectedUrl), eq(String.class));
    }

    @Test
    void rebuildIndex_qleverReturnsError() {
        String expectedUrl = QLEVER_URL + "?cmd=rebuild-index&access-token=" + ACCESS_TOKEN;

        ResponseEntity<String> errorResponse = new ResponseEntity<>("Rebuild failed", HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.getForEntity(eq(expectedUrl), eq(String.class)))
                .thenReturn(errorResponse);

        IOException thrown = assertThrows(IOException.class, () -> qleverClient.rebuildIndex());
        assertTrue(thrown.getMessage().contains("QLever returned error for index rebuild"));
        assertTrue(thrown.getMessage().contains("Status: 500 INTERNAL_SERVER_ERROR"));
        assertTrue(thrown.getMessage().contains("Body: Rebuild failed"));
    }

    @Test
    void rebuildIndex_restClientException() {
        String expectedUrl = QLEVER_URL + "?cmd=rebuild-index&access-token=" + ACCESS_TOKEN;

        when(restTemplate.getForEntity(eq(expectedUrl), eq(String.class)))
                .thenThrow(new RestClientException("Network unreachable"));

        IOException thrown = assertThrows(IOException.class, () -> qleverClient.rebuildIndex());
        assertTrue(thrown.getMessage().contains("Failed to trigger QLever index rebuild"));
        assertTrue(thrown.getMessage().contains("Cause: Network unreachable"));
        assertNotNull(thrown.getCause());
        assertTrue(thrown.getCause() instanceof RestClientException);
    }
}