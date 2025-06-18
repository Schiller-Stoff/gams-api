package org.zim.gamsapi.System;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zim.gamsapi.DigitalObject.DigitalObjectDublinCoreSpecification;
import org.zim.gamsapi.DigitalObject.dto.DigitalObjectSearchResultDTO;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.DigitalObject.interfaces.IDigitalObjectService;
import org.zim.gamsapi.System.config.OpenAPIConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Controller for searching digital objects.
 */
@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping({"/api/v1/search" })
@Tag(name = OpenAPIConfig.SEARCH_TAG, description = OpenAPIConfig.SEARCH_TAG_DESCRIPTION)
public class SearchController {
  /**
   * Service for searching digital objects.
   */
  private final IDigitalObjectService digitalObjectService;

  /**
   * Fulltext search over all dublin core fields of a digital object.
   * @param projects list of project abbreviations
   * @param dcFields list of DublinCoreElement names
   * @param search fulltext search string
   * @param pageIndex page index
   * @param pageSize page size
   * @return a page of digital objects
   */
  @GetMapping(path = "/dc/fulltext", produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE
  })
  @ResponseBody
  @Operation(
      summary = "Dublin core fulltext search based on digital objects and multiple projects.",
      description = "Searches for digital objects based on a fulltext search over all Dublin Core fields. The search is performed on multiple projects and can include multiple Dublin Core fields.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Digital objects found",
              content = @Content(mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "400", description = "Invalid request parameters",
              content = @Content)
      }
  )
  public Page<DigitalObjectListItemView> searchDigitalObjectsViaDublinCoreFulltext(
      @RequestParam Set<String> projects,
      // dublin core search parameters
      @RequestParam(
          required = false,
          // sets default value empty set
          defaultValue = ""
      ) Set<String> dcFields,
      @RequestParam String search,
      // for pagination
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "20") int pageSize
  ){

    // limit page size
    if (pageSize >= 100) {
      pageSize = 100;
    }

    //TODO include dublin core data into response?

    return digitalObjectService.searchByDCFulltext(
        projects,
        dcFields,
        search,
        PageRequest.of(pageIndex, pageSize)
    );
  }

  @GetMapping(path = "/dc", produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE
  })
  @ResponseBody
  @Operation(
      summary = "Advanced multi-criteria Dublin Core search for digital objects",
      description = "Advanced search supporting multiple Dublin Core criteria with different search modes. " +
          "Supports exact match, contains, and fulltext search modes.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Digital objects found",
              content = @Content(mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "400", description = "Invalid request parameters",
              content = @Content)
      }
  )
  @Parameter(
      name = "dcEntries",
      required = false,
      examples = {
          @ExampleObject(
              name = "dc.type search",
              summary = "Return all dc.type fields with value 'Brief'",
              value = "{\"dc.type\": [\"Brief\"]}",
              description = "Search for type field entries"
          ),
          @ExampleObject(
              name = "Multi-field search",
              summary = "Search across multiple DC fields",
              value = "{\"dc.type\": [\"Brief\"], \"dc.subject\": [\"test\"], \"dc.language\": [\"en\"]}",
              description = "Combined search across multiple fields"
          )
      },
      description = "Multi-value map of Dublin Core entries to filter by.",
      schema = @Schema(type = "object")
  )
  public Page<DigitalObjectSearchResultDTO> searchDigitalObjectsByDublinCoreAdvanced(
      @RequestParam MultiValueMap<String, String> dcCriteria,
      @RequestParam Set<String> projects,
      @RequestParam(defaultValue = "EXACT_MATCH") DigitalObjectDublinCoreSpecification.SearchMode searchMode,
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "20") int pageSize) {

    pageSize = Math.min(pageSize, 20); // Limit page size

    // includes now all request parameters, not just "dc.*" ones
    // only keep parameters keys that start with "dc."
    var filteredDcFields = new HashMap<String, List<String>>();
    dcCriteria.forEach((key, values) -> {
      if (key.startsWith("dc.")) {
        String newKey = key.substring(3); // Remove "dc." prefix
        filteredDcFields.put(newKey, values);
      }
    });

    log.debug("Advanced DC search - criteria: {}, projects: {}, mode: {}",
        dcCriteria, projects, searchMode);

    return digitalObjectService.searchDigitalObjectsByDublinCoreCriteria(
        MultiValueMap.fromMultiValue(filteredDcFields), projects, searchMode, PageRequest.of(pageIndex, pageSize));
  }

}
