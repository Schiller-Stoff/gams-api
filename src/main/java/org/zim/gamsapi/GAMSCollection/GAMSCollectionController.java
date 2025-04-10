package org.zim.gamsapi.GAMSCollection;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.GAMSCollection.interfaces.GAMSCollectionDetailsView;
import org.zim.gamsapi.GAMSCollection.interfaces.GamsCollectionCompactView;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.interfaces.IDigitalObjectService;
import org.zim.gamsapi.Project.interfaces.IProjectService;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping({"/api/v1/"})
@Slf4j
@RequiredArgsConstructor
public class GAMSCollectionController {

  private final IGAMSCollectionService collectionService;
  private final IProjectService projectService;
  private final IDigitalObjectService digitalObjectService;

  @GetMapping(produces = MimeTypeUtils.APPLICATION_JSON_VALUE, value = {"/collections", "/collections/"})
  @ResponseBody
  @Operation(summary = "Get all collections")
  public Page<GamsCollectionCompactView> getAllCollections() {
    log.error("TRIGGERED CONTROLLER Page<CollectionCompactView> getAllCollections");
    // TODO redo pageing parameters!
    return collectionService.findAll(PageRequest.of(0,1000));
  }

  @GetMapping(value = "/collections/{id}", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Operation(summary = "Get a collection by ID")
  public GAMSCollectionDetailsView getCollection(@PathVariable String id) {
    return collectionService.findById(id);
  }

  @PutMapping(value = "/collections/{id}")
  @ResponseBody
  @Operation(summary = "Create or update a collection")
  public ResponseEntity<GAMSCollection> createOrUpdateCollection(
      @PathVariable String id,
      @RequestBody GAMSCollection GAMSCollection,
      @RequestHeader Map<String, String> requestHeader) {

    // Ensure the ID in the path matches the ID in the body
    if (!id.equals(GAMSCollection.getId())) {
      GAMSCollection.setId(id);
    }

    GAMSCollection savedGAMSCollection = collectionService.save(GAMSCollection);
    return ResponseEntity.ok(savedGAMSCollection);
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
  public GAMSCollection addDigitalObjectToCollection(
      @PathVariable String id,
      @PathVariable String objectId) {
    return collectionService.addDigitalObjectToCollection(id, objectId);
  }



  @GetMapping(value = "/collections/{id}/objects", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Operation(summary = "Get all digital objects in a collection")
  public Page<DigitalObjectListItemView> getCollectionObjects(@PathVariable String id) {
    // TODO think about pagination params!
    return collectionService.findDigitalObjectsByCollectionId(id, PageRequest.of(0,10000));
  }

  @GetMapping(value = "/projects/{projectAbbr}/collections", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Operation(summary = "Get all collections owned by a project")
  public Page<GamsCollectionCompactView> getCollectionsByProject(@PathVariable String projectAbbr) {
    // TODO handle pageSie parameters
    return collectionService.findByProjectAbbr(projectAbbr, PageRequest.of(0,1000));
  }

  @GetMapping(value = "/collections/objects/{objectId}", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Operation(summary = "Get all collections containing a specific digital object")
  public List<GAMSCollection> getCollectionsByDigitalObject(@PathVariable String objectId) {
    return collectionService.findByDigitalObject(objectId);
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
