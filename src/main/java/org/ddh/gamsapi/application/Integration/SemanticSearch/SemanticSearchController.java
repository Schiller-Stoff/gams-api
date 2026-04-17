package org.ddh.gamsapi.application.Integration.SemanticSearch;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.Common.interfaces.IIntegrationController;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;


@Controller
@RequestMapping
@Slf4j
@RequiredArgsConstructor
@Tag(name = OpenAPIConfig.INTEGRATION_TAG, description = OpenAPIConfig.INTEGRATION_TAG_DESCRIPTION)
public class SemanticSearchController implements IIntegrationController {

  public static final String SEMANTIC_SEARCH_GET_PATH = "/api/integration/v1/semantic-search";

  public static final String SEMANTIC_SEARCH_MANAGEMENT_PATH = SEMANTIC_SEARCH_GET_PATH + "/projects/{projectAbbr}/objects";

  public static final String SEMANTIC_SEARCH_SINGLE_OBJECT_MANAGEMENT_PATH = SEMANTIC_SEARCH_MANAGEMENT_PATH + "/{id}";

  private final SemanticSearchService semanticSearchService;


  @Operation(
      summary = "Index all project objects in the semantic search service",
      description = "Indexes all SEMANTIC_STATEMENTS.ttl datastreams of a project into the QLever triplestore. " +
          "Drops the existing project graph and rebuilds it from scratch."
  )
  @PostMapping(SEMANTIC_SEARCH_MANAGEMENT_PATH)
  @ResponseBody
  @Override
  public void indexProjectObjects(@PathVariable String projectAbbr) {
    log.debug("*** Indexing all objects for project {} in semantic search", projectAbbr);
    semanticSearchService.indexObjects(projectAbbr);
  }

  @Hidden
  @PostMapping(value = SEMANTIC_SEARCH_MANAGEMENT_PATH, produces = MimeTypeUtils.TEXT_HTML_VALUE)
  public String indexProjectObjectsHtml(@PathVariable String projectAbbr) {
    log.debug("*** HTML: Indexing all objects for project {} in semantic search", projectAbbr);
    semanticSearchService.indexObjects(projectAbbr);
    return "redirect:/api/curation/v1/projects/" + projectAbbr + "/objects";
  }

  @Operation(
      summary = "Delete all project objects from the semantic search service",
      description = "Drops the project's named graph from the QLever triplestore, " +
          "removing all indexed semantic statements for that project."
  )
  @DeleteMapping(SEMANTIC_SEARCH_MANAGEMENT_PATH)
  @ResponseBody
  @Override
  public void deleteProjectObjects(@PathVariable String projectAbbr) {
    log.debug("*** Deleting all objects for project {} from semantic search", projectAbbr);
    semanticSearchService.deleteIndexedObjects(projectAbbr);
  }

  @Hidden
  @DeleteMapping(value = SEMANTIC_SEARCH_MANAGEMENT_PATH, produces = MimeTypeUtils.TEXT_HTML_VALUE)
  public String deleteProjectObjectsHtml(@PathVariable String projectAbbr) {
    log.debug("*** HTML: Deleting all objects for project {} from semantic search", projectAbbr);
    semanticSearchService.deleteIndexedObjects(projectAbbr);
    return "redirect:/api/curation/v1/projects/" + projectAbbr + "/objects";
  }


  // ---------------------------------------------------------------------------
  // Single object operations
  // ---------------------------------------------------------------------------

  @Operation(
      summary = "Index a single object in the semantic search service",
      description = "Indexes the SEMANTIC_STATEMENTS.ttl datastream of a single digital object " +
          "into the project's named graph in QLever. Does not drop existing project data."
  )
  @PostMapping(value = SEMANTIC_SEARCH_SINGLE_OBJECT_MANAGEMENT_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  public void indexObject(@PathVariable String projectAbbr, @PathVariable String id) {
    log.debug("*** Indexing object {} for project {} in semantic search", id, projectAbbr);
    semanticSearchService.indexObject(projectAbbr, id);
  }

  @Hidden
  @PostMapping(value = SEMANTIC_SEARCH_SINGLE_OBJECT_MANAGEMENT_PATH, produces = MimeTypeUtils.TEXT_HTML_VALUE)
  public String indexObjectHtml(@PathVariable String projectAbbr, @PathVariable String id) {
    log.debug("*** HTML: Indexing object {} for project {} in semantic search", id, projectAbbr);
    semanticSearchService.indexObject(projectAbbr, id);
    return "redirect:/api/curation/v1/projects/" + projectAbbr + "/objects/" + id;
  }

  @Operation(
      summary = "Delete a single object from the semantic search service",
      description = "Removes all triples for a single digital object from the project's " +
          "named graph in QLever."
  )
  @DeleteMapping(value = SEMANTIC_SEARCH_SINGLE_OBJECT_MANAGEMENT_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  public void deleteObject(@PathVariable String projectAbbr, @PathVariable String id) {
    log.debug("*** Deleting object {} for project {} from semantic search", id, projectAbbr);
    semanticSearchService.deleteIndexedObject(projectAbbr, id);
  }

  @Hidden
  @DeleteMapping(value = SEMANTIC_SEARCH_SINGLE_OBJECT_MANAGEMENT_PATH, produces = MimeTypeUtils.TEXT_HTML_VALUE)
  public String deleteObjectHtml(@PathVariable String projectAbbr, @PathVariable String id) {
    log.debug("*** HTML: Deleting object {} for project {} from semantic search", id, projectAbbr);
    semanticSearchService.deleteIndexedObject(projectAbbr, id);
    return "redirect:/api/curation/v1/projects/" + projectAbbr + "/objects/" + id;
  }

}