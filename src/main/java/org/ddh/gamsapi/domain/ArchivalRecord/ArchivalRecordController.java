package org.ddh.gamsapi.domain.ArchivalRecord;

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

import java.util.List;

@RestController
@RequestMapping(value = { "/api/curation/v1/archival-records" }) //TODO check auth!!! spring security config
@Slf4j
@RequiredArgsConstructor
@Tag(name = OpenAPIConfig.DIGITAL_OBJECTS_TAG, description = OpenAPIConfig.DIGITAL_OBJECTS_TAG_DESCRIPTION) // TODO redo openapi
public class ArchivalRecordController {

  private final IArchivalRecordService archivalRecordService;
  private final IProjectService projectService;

  // TODO add to openapi the request param
  @Operation(
      summary = "Get archival records",
      description = "Retrieve the archival records optionally for specific digital objects.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Successful retrieval of the archival records",
              content = @Content)
      }
  )
  @GetMapping
  public List<ArchivalRecordCompactView> findArchivalRecords(
      @RequestParam String objectId
  ) {
    return archivalRecordService.findForObject(objectId);
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
  public void createArchivalRecord(
      @RequestBody @Valid ArchivalRecordReserveDto archivalRecord
  ){
    // TODO implement
  }

  // TODO update open-api annotation
  @DeleteMapping(path = "/{*pid}") // pattern takes everything after (but includes the leading slash!)
  @Operation(
      summary = "Deletes a archival record by it's pid.",
      description = "Allows to delete an archival record by the archival record pid.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Archival record successfully deleted",
              content = @Content)
      }
  )
  public void delete(
      @PathVariable String pid
  ){
    // need to check for the included leading slash from the PathVariable
    String normalizedPid = pid.startsWith("/") ? pid.substring(1) : pid;

    // TODO validate pids!
    archivalRecordService.deleteById(normalizedPid);
  }

}
