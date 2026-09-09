package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectService;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping(value = { "/api/curation/v1/projects/{projectAbbr}/objects/{id}/archival-records" })
@Slf4j
@RequiredArgsConstructor
@Tag(name = OpenAPIConfig.DIGITAL_OBJECTS_TAG, description = OpenAPIConfig.DIGITAL_OBJECTS_TAG_DESCRIPTION)
public class ArchivalRecordController {

  private final IArchivalRecordService archivalRecordService;
  private final IProjectService projectService;

  @Operation(
      summary = "Get archival records",
      description = "Retrieve the archival records associated with a specific digital object within a project.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Successful retrieval of the archival records",
              content = @Content)
      }
  )
  @RequestMapping(method = RequestMethod.GET)
  @ResponseBody
  public List<ArchivalRecordCompactView> findArchivalRecords(
      @PathVariable String projectAbbr,
      @PathVariable String id
  ) {
    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);
    return archivalRecordService.findForObject(id);
  }

  @Operation(
      summary = "Get only public archival records",
      description = "Retrieve the public archival records associated with a specific digital object within a project.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Successful retrieval of the archival records",
              content = @Content)
      }
  )
  @RequestMapping(method = RequestMethod.GET, path = "/public")
  @ResponseBody
  public List<ArchivalRecordCompactView> findArchivalRecordsByArchivingStatus(
      @PathVariable String projectAbbr,
      @PathVariable String id
  ) {
    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);
    return archivalRecordService.findForObjectByArchivingStatus(id, ArchivingStatus.PUBLISHED);
  }

  @PostMapping
  @ResponseBody
  @Operation(
      summary = "Create a an archival record for a digital object",
      description = "Allows to create an archival record for a specific digital object by providing the project abbreviation in the path variable, the digital object ID, and the archival record data in the request body.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Archival record successfully created",
              content = @Content)
      }
  )
  public void reserveArchivalRecord(
      @PathVariable String projectAbbr,
      @PathVariable String id,
      @RequestBody @Valid ArchivalRecordReserveDto archivalRecord
  ){
    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);

    archivalRecordService.reserve(id, archivalRecord);
  }

  @DeleteMapping(path = "/{recordId}")
  @ResponseBody
  @Operation(
      summary = "Deletes a archival record for a digital object",
      description = "Allows to delete an archival record for a specific digital object by providing the project abbreviation in the path variable, the digital object ID, and the archival record id.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Archival record successfully deleted",
              content = @Content)
      }
  )
  public void delete(
      @PathVariable String projectAbbr,
      @PathVariable String id,
      @PathVariable Long recordId
  ){
    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);
    archivalRecordService.deleteById(recordId);

  }

  @Operation(
      summary = "Brings a reserved archival record in the draft state.",
      description = "Allows to draft an archival record for a specific digital object by providing the project abbreviation in the path variable, the digital object ID, and the archival record data in the request body.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Archival record successfully patched",
              content = @Content)
      }
  )
  @PatchMapping(path = "/draft")
  @ResponseBody
  public void draftArchivalRecord(
      @PathVariable String projectAbbr,
      @PathVariable String id,
      @RequestBody @Valid ArchivalRecordDraftDto archivalRecord
  ){
    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);
    archivalRecordService.draftArchivalRecord(id, archivalRecord);
  }

  @Operation(
      summary = "Brings a drafted archival record in the published state.",
      description = "Allows to publish an archival record for a specific digital object by providing the project abbreviation in the path variable, the digital object ID, and the archival record data in the request body.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Archival record successfully published",
              content = @Content)
      }
  )
  @PatchMapping(path = "/publish")
  @ResponseBody
  public void publishArchivalRecord(
      @PathVariable String projectAbbr,
      @PathVariable String id,
      @RequestBody @Valid ArchivalRecordPublishDto archivalRecord
  ){
    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);
    archivalRecordService.publishArchivalRecord(id, archivalRecord);

  }

}
