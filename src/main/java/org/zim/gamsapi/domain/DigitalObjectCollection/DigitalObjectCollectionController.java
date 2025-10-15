package org.zim.gamsapi.domain.DigitalObjectCollection;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.domain.DigitalObject.utils.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.domain.DigitalObjectCollection.interfaces.DigitalObjectCollectionDetailsView;
import org.zim.gamsapi.domain.DigitalObjectCollection.interfaces.DigitalObjectCollectionCompactView;
import org.zim.gamsapi.domain.Project.ProjectBuilder;
import org.zim.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.zim.gamsapi.infrastructure.System.dto.PagedResponse;

@Controller
@RequestMapping({"/api/v1"})
@Slf4j
@RequiredArgsConstructor
@Tag(name = OpenAPIConfig.GAMS_COLLECTIONS_TAG, description = OpenAPIConfig.GAMS_COLLECTIONS_TAG_DESCRIPTION)
public class DigitalObjectCollectionController {

  private final IDigitalObjectCollectionService collectionService;

  @GetMapping(produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE,
  }, value = {"/collections" })
  @ResponseBody
  @Operation(summary = "Get all collections")
  public PagedResponse<DigitalObjectCollectionCompactView> getAllCollections(
      // for pagination
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "100") int pageSize,
      @RequestParam(defaultValue = "id") String sortBy
  ) {
    // limit pageSize to max 100
    if (pageSize >= 100) {
      pageSize = 100;
    }
    // TODO redo pageing parameters!
    return collectionService.findAll(
        PageRequest.of(pageIndex, pageSize, Sort.by(sortBy))
    );
  }

  @GetMapping(value = "/collections/{id}", produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE
  })
  @ResponseBody
  @Operation(summary = "Get a collection by ID")
  public DigitalObjectCollectionDetailsView getCollection(@PathVariable String id) {
    return collectionService.findById(id);
  }

  @PutMapping(value = "/projects/{projectAbbr}/collections/{id}")
  @ResponseBody
  @Operation(summary = "Create a GAMS collection")
  public void createCollection(
      @PathVariable String id,
      @RequestBody CreateDigitalObjectCollectionDTO createDigitalObjectCollectionDTO
  ) {
    DigitalObjectCollection digitalObjectCollection = DigitalObjectCollection.builder()
        .id(id)
        .project(ProjectBuilder.builder().projectAbbr(createDigitalObjectCollectionDTO.getProjectAbbr()).build())
        .title(createDigitalObjectCollectionDTO.getTitle())
        .description(createDigitalObjectCollectionDTO.getDescription())
        .build();

    collectionService.save(digitalObjectCollection);
  }

  @PatchMapping(value = "/projects/{projectAbbr}/collections/{id}")
  @ResponseBody
  @Operation(summary = "Update basic metadata of a gams collection like title or description")
  public void updateCollection(
      @PathVariable String id,
      @RequestBody CreateDigitalObjectCollectionDTO createDigitalObjectCollectionDTO
  ) {
    DigitalObjectCollection digitalObjectCollection = DigitalObjectCollection.builder()
        .id(id)
        .project(ProjectBuilder.builder().projectAbbr(createDigitalObjectCollectionDTO.getProjectAbbr()).build())
        .title(createDigitalObjectCollectionDTO.getTitle())
        .description(createDigitalObjectCollectionDTO.getDescription())
        .build();
    collectionService.updateMetadata(digitalObjectCollection);
  }

  @DeleteMapping(value = "/projects/{projectAbbr}/collections/{id}")
  @ResponseBody
  @Operation(summary = "Delete a collection")
  public ResponseEntity<Void> deleteCollection(@PathVariable String id) {
    collectionService.deleteById(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping(value = "/projects/{projectAbbr}/collections/{id}/objects/{objectId}")
  @ResponseBody
  @Operation(summary = "Add a digital object to a collection")
  public void addDigitalObjectToCollection(
      @PathVariable String id,
      @PathVariable String objectId) {
    collectionService.addDigitalObjectToCollection(id, objectId);
  }



  @GetMapping(value = "/collections/{id}/objects", produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE
  })
  @ResponseBody
  @Operation(summary = "Get all digital objects in a collection")
  public PagedResponse<DigitalObjectListItemView> getCollectionObjects(
      @PathVariable String id,
      // for pagination
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "100") int pageSize,
      @RequestParam(defaultValue = "id") String sortBy
  ) {
    // limit pageSize to max 100
    if (pageSize >= 100) {
      pageSize = 100;
    }

    return collectionService.findDigitalObjectsByCollectionId(
        id,
        PageRequest.of(pageIndex, pageSize, Sort.by(sortBy))
    );
  }

  @GetMapping(value = "/projects/{projectAbbr}/collections", produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE,
  })
  @ResponseBody
  @Operation(summary = "Get all collections owned by a project")
  public PagedResponse<DigitalObjectCollectionCompactView> getCollectionsByProject(
      @PathVariable String projectAbbr,
      // for pagination
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "100") int pageSize,
      @RequestParam(defaultValue = "id") String sortBy
  ) {
    // limit pageSize to max 100
    if (pageSize >= 100) {
      pageSize = 100;
    }

    return collectionService.findByProjectAbbr(
        projectAbbr,
        PageRequest.of(pageIndex, pageSize, Sort.by(sortBy))
    );
  }

  @GetMapping(value = "/projects/{projectAbbr}/objects/{id}/collections", produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE
  })
  @ResponseBody
  @Operation(summary = "Get all collections containing a specific digital object")
  public PagedResponse<DigitalObjectCollectionCompactView> getCollectionsByDigitalObject(
      @PathVariable String id,
      // for pagination
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "100") int pageSize,
      @RequestParam(defaultValue = "id") String sortBy
  ) {
    // limit pageSize to max 100
    if (pageSize >= 100) {
      pageSize = 100;
    }

    return collectionService.findByDigitalObject(
        id,
        PageRequest.of(pageIndex, pageSize, Sort.by(sortBy))
    );
  }

  @DeleteMapping(value = "/projects/{projectAbbr}/collections/{id}/objects/{objectId}")
  @ResponseBody
  @Operation(summary = "Remove a digital object from a collection")
  public ResponseEntity<Void> removeDigitalObjectFromCollection(
      @PathVariable String id,
      @PathVariable String objectId) {
    collectionService.removeDigitalObjectFromCollection(id, objectId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping(value = "/{id}", produces = MimeTypeUtils.TEXT_HTML_VALUE)
  public String getCollectionView(@PathVariable String id, Model model) {
    // TODO implement!
    //GAMSCollection GAMSCollection = collectionService.findById(id);
    //model.addAttribute("collection", GAMSCollection);
    return "Collection/show";
  }
}
