package org.zim.gamsapi.GAMSCollection;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.GAMSCollection.interfaces.GAMSCollectionDetailsView;
import org.zim.gamsapi.GAMSCollection.interfaces.GamsCollectionCompactView;
import org.zim.gamsapi.DigitalObject.interfaces.IDigitalObjectService;
import org.zim.gamsapi.Project.ProjectBuilder;
import org.zim.gamsapi.Project.interfaces.IProjectService;

@Controller
@RequestMapping({"/api/v1/"})
@Slf4j
@RequiredArgsConstructor
public class GAMSCollectionController {

  private final IGAMSCollectionService collectionService;

  @GetMapping(produces = MimeTypeUtils.APPLICATION_JSON_VALUE, value = {"/collections", "/collections/"})
  @ResponseBody
  @Operation(summary = "Get all collections")
  public Page<GamsCollectionCompactView> getAllCollections(
      // for pagination
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "100") int pageSize,
      @RequestParam(defaultValue = "id") String sortBy
  ) {
    // limit pageSize to max 100
    if (pageSize >= 100) {
      pageSize = 100;
    }

    log.error("TRIGGERED CONTROLLER Page<CollectionCompactView> getAllCollections");
    // TODO redo pageing parameters!
    return collectionService.findAll(PageRequest.of(pageIndex, pageSize, Sort.by(sortBy)));
  }

  @GetMapping(value = "/collections/{id}", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Operation(summary = "Get a collection by ID")
  public GAMSCollectionDetailsView getCollection(@PathVariable String id) {
    return collectionService.findById(id);
  }

  @PutMapping(value = "/collections/{id}")
  @ResponseBody
  @Operation(summary = "Create a GAMS collection")
  public void createCollection(
      @PathVariable String id,
      @RequestBody CreateGAMSCollectionDTO createGAMSCollectionDTO
  ) {
    GAMSCollection gamsCollection = GAMSCollection.builder()
        .id(id)
        .project(ProjectBuilder.builder().projectAbbr(createGAMSCollectionDTO.getProjectAbbr()).build())
        .title(createGAMSCollectionDTO.getTitle())
        .description(createGAMSCollectionDTO.getDescription())
        .build();

    collectionService.save(gamsCollection);
  }

  @PatchMapping(value = "/collections/{id}")
  @ResponseBody
  @Operation(summary = "Update basic metadata of a gams collection like title or description")
  public void updateCollection(
      @PathVariable String id,
      @RequestBody CreateGAMSCollectionDTO createGAMSCollectionDTO
  ) {
    GAMSCollection gamsCollection = GAMSCollection.builder()
        .id(id)
        .project(ProjectBuilder.builder().projectAbbr(createGAMSCollectionDTO.getProjectAbbr()).build())
        .title(createGAMSCollectionDTO.getTitle())
        .description(createGAMSCollectionDTO.getDescription())
        .build();
    collectionService.updateMetadata(gamsCollection);
  }

  @DeleteMapping(value = "/collections/{id}")
  @ResponseBody
  @Operation(summary = "Delete a collection")
  public ResponseEntity<Void> deleteCollection(@PathVariable String id) {
    GAMSCollectionDetailsView GAMSCollection = collectionService.findById(id);
    // TODO implement
    //collectionService.delete(GAMSCollection);
    return ResponseEntity.noContent().build();
  }

  @PostMapping(value = "/collections/{id}/objects/{objectId}")
  @ResponseBody
  @Operation(summary = "Add a digital object to a collection")
  public void addDigitalObjectToCollection(
      @PathVariable String id,
      @PathVariable String objectId) {
    collectionService.addDigitalObjectToCollection(id, objectId);
  }



  @GetMapping(value = "/collections/{id}/objects", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Operation(summary = "Get all digital objects in a collection")
  public Page<DigitalObjectListItemView> getCollectionObjects(
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

  @GetMapping(value = "/projects/{projectAbbr}/collections", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Operation(summary = "Get all collections owned by a project")
  public Page<GamsCollectionCompactView> getCollectionsByProject(
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

  @GetMapping(value = "/projects/{projectAbbr}/objects/{id}/collections", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Operation(summary = "Get all collections containing a specific digital object")
  public Page<GamsCollectionCompactView> getCollectionsByDigitalObject(
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

  @DeleteMapping(value = "/collections/{id}/objects/{objectId}")
  @ResponseBody
  @Operation(summary = "Remove a digital object from a collection")
  public GAMSCollection removeDigitalObjectFromCollection(
      @PathVariable String id,
      @PathVariable String objectId) {
    return collectionService.removeDigitalObjectFromCollection(id, objectId);
  }

  @GetMapping(value = "/{id}", produces = MimeTypeUtils.TEXT_HTML_VALUE)
  public String getCollectionView(@PathVariable String id, Model model) {
    // TODO implement!
    //GAMSCollection GAMSCollection = collectionService.findById(id);
    //model.addAttribute("collection", GAMSCollection);
    return "Collection/show";
  }
}
