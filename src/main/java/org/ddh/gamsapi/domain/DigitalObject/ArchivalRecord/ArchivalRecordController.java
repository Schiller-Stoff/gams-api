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
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = { "/api/curation/v1/archival-records" }) //TODO check auth!!! spring security config
@Slf4j
@RequiredArgsConstructor
@Tag(name = OpenAPIConfig.DIGITAL_OBJECTS_TAG, description = OpenAPIConfig.DIGITAL_OBJECTS_TAG_DESCRIPTION) // TODO redo openapi
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
  @GetMapping
  public List<ArchivalRecordCompactView> findArchivalRecords(

  ) {
    // TODO implement
    return null;
  }

  @Operation(
      summary = "Get only public archival records",
      description = "Retrieve the public archival records associated with a specific digital object within a project.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Successful retrieval of the archival records",
              content = @Content)
      }
  )
  @GetMapping(path = "/public")
  public List<ArchivalRecordCompactView> findArchivalRecordsByArchivingStatus(
  ) {
   // TODO implement
    return new ArrayList<>();
  }

  @PostMapping
  @Operation(
      summary = "Create a an archival record for a digital object",
      description = "Allows to create an archival record for a specific digital object by providing the project abbreviation in the path variable, the digital object ID, and the archival record data in the request body.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Archival record successfully created",
              content = @Content)
      }
  )
  public void reserveArchivalRecord(
      @RequestBody @Valid ArchivalRecordReserveDto archivalRecord
  ){

  }

  @DeleteMapping(path = "/{recordId}")
  @Operation(
      summary = "Deletes a archival record for a digital object",
      description = "Allows to delete an archival record for a specific digital object by providing the project abbreviation in the path variable, the digital object ID, and the archival record id.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Archival record successfully deleted",
              content = @Content)
      }
  )
  public void delete(
      @PathVariable Long recordId
  ){

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
  public void draftArchivalRecord(
      @RequestBody @Valid ArchivalRecordDraftDto archivalRecord
  ){

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
  public void publishArchivalRecord(
      @RequestBody @Valid ArchivalRecordPublishDto archivalRecord
  ){
    

  }

}
