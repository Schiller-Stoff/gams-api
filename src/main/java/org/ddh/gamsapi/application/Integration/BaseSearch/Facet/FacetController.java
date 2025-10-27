package org.ddh.gamsapi.application.Integration.BaseSearch.Facet;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearchProperties;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationServiceException;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Controller for handling faceted search requests for digital objects.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = OpenAPIConfig.INTEGRATION_TAG, description = OpenAPIConfig.INTEGRATION_TAG_DESCRIPTION)
public class FacetController {

  public static final String FACET_SEARCH_PATH = "/api/v1/integration/gsearch/facets";

  private final FacetService facetService;

  @GetMapping(path = FACET_SEARCH_PATH, produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE
  })
  @ResponseBody
  public FacetResponseDTO searchDigitalObjectsByDublinCoreAdvanced(
      @RequestParam MultiValueMap<String, String> dcCriteria,
      @RequestParam Set<String> projects,
      @RequestParam(required = false, defaultValue = "", name = "q") String fulltextQuery,
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false, defaultValue = "dc.title") String sortBy,
      @RequestParam(required = false, defaultValue = "asc") String sortDir
  ) {

    // TODO is this still needed?
    pageSize = Math.min(pageSize, 50); // Limit page size

    // includes now all request parameters, not just "dc.*" ones
    // only keep parameters keys that start with "dc."
    var filteredDcFields = new HashMap<String, List<String>>();
    dcCriteria.forEach((key, values) -> {
      if (key.startsWith("dc.")) {
        // TODO WTF AM I REMOVING THE dc. PREFIX HERE???
        String newKey = key.substring(3); // Remove "dc." prefix
        filteredDcFields.put(newKey, values);
      }
    });

    log.debug("Advanced DC search - criteria: {}, projects: {}",
        dcCriteria, projects);

    // TODO add tests for sorting procedure!
    PageRequest pageRequest = buildPageRequest(pageIndex, pageSize, sortBy, sortDir);

    return facetService.facetSearch(
        projects,
        fulltextQuery,
        MultiValueMap.fromMultiValue(filteredDcFields),
        pageRequest
    );
  }


  /**
   * Builds PageRequest with validated sort parameters.
   *
   * @param pageIndex Zero-based page index
   * @param pageSize Number of results per page
   * @param sortBy TODO
   * @param sortDir TODO
   * @return PageRequest with validated sort
   * @throws IllegalArgumentException if sort field is not allowed
   */
  private PageRequest buildPageRequest(int pageIndex, int pageSize, String sortBy, String sortDir) {
    if (sortBy == null || sortBy.isEmpty()) {
      // Default: no explicit sorting (Solr relevance score)
      return PageRequest.of(pageIndex, pageSize);
    }

    String solrField = mapAndValidateSortField(sortBy);

    return PageRequest.of(pageIndex, pageSize, Sort.by(
        Sort.Direction.fromString(sortDir), solrField)
    );

  }

  /**
   * TODO
   *
   *
   */
  private String mapAndValidateSortField(String userField) {

    List<String> allowedSortFields = List.of(
        BaseSearchProperties.TITLE.name,
        BaseSearchProperties.CREATOR.name,
        BaseSearchProperties.PUBLISHER.name,
        BaseSearchProperties.OBJECT_ID.name,
        "dc.title",
        "dc.creator",
        "dc.publisher",
        "dc.type",
        "dc.identifier",
        "dc.date",
        "dc.subject",
        "dc.language",
        "dc.format",
        "dc.rights",
        "dc.coverage",
        "dc.description",
        BaseSearchProperties.TYPE.name,
        BaseSearchProperties.PROJECT.name,
        BaseSearchProperties.DATASTREAMS.name
    );

    if (!allowedSortFields.contains(userField)) {
      String msg = String.format(
          "Sorting by field '%s' is not allowed. Allowed fields are: %s",
          userField,
          String.join(", ", allowedSortFields)
      );
      log.warn(msg);
      // TODO wrong exception - should be a user side error status code!
      throw new IntegrationServiceException(msg);
    }

    return userField;
  }

}
