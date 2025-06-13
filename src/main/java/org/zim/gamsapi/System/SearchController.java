package org.zim.gamsapi.System;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.DigitalObject.interfaces.IDigitalObjectService;
import org.zim.gamsapi.System.config.OpenAPIConfig;
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
  @GetMapping(path = "/dc/fulltext", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
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

    return digitalObjectService.searchByDCFulltext(
        projects,
        dcFields,
        search,
        PageRequest.of(pageIndex, pageSize)
    );

  }


  /**
   * Search for digital objects based on Dublin Core metadata.
   * @param projectAbbrs list of project abbreviations
   * @param dcField name of the DublinCoreElement on which to search
   * @param search list of values of the DublinCoreElement
   * @param pageIndex page index
   * @param pageSize page size
   * @return a page of digital objects
   */
  @GetMapping(path = "/dc", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Operation(
      summary = "Dublin core search based on digital objects and different projects.",
      description = "Searches for digital objects based on Dublin Core metadata. The search is performed on a specific Dublin Core field with exact match.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Digital objects found",
              content = @Content(mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "400", description = "Invalid request parameters",
              content = @Content)
      }
  )
  public Page<DigitalObjectListItemView> searchDigitalObjectsViaDublinCoreExactMatch(
      @RequestParam Set<String> projectAbbrs,
      @RequestParam String dcField,
      @RequestParam @NotEmpty List<String> search,
      // for pagination
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "20") int pageSize
  ){

    // limit page size
    if (pageSize >= 100) {
      pageSize = 100;
    }

    return digitalObjectService.searchObjectsByDublincCoreTags(
        projectAbbrs, dcField, search, PageRequest.of(pageIndex, pageSize)
    );

  }


}
