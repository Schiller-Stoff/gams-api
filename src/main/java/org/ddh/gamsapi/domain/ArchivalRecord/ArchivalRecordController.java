package org.ddh.gamsapi.domain.ArchivalRecord;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.dto.ArchivalRecordDraftDto;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.dto.ArchivalRecordPublishDto;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectService;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(value = { "/api/curation/v1/archival-records" }) //TODO check auth!!! spring security config
@Slf4j
@RequiredArgsConstructor
@Tag(name = OpenAPIConfig.DIGITAL_OBJECTS_TAG, description = OpenAPIConfig.DIGITAL_OBJECTS_TAG_DESCRIPTION) // TODO redo openapi
public class ArchivalRecordController {

  private final IArchivalRecordService archivalRecordService;
  private final IProjectService projectService;
  private final Validator validator;

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
      summary = "Reserve an archival record for a digital object",
      description = "Allows to create an archival record for a specific digital object by providing the project abbreviation in the path variable, the digital object ID, and the archival record data in the request body.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Archival record successfully created",
              content = @Content)
      }
  )
  public ArchivalRecord reserveArchivalRecordByObjectId(
      @RequestParam String objectId
  ){
    return archivalRecordService.reserveArchivalRecordByObjectId(objectId);
  }

  //TODO open api annotations
  @PutMapping(path = "/{*pid}")
  public ArchivalRecord reserveArchivalRecordViaPid(
      @PathVariable String pid,
      @RequestParam String objectId
  ){

    // need to check for the included leading slash from the PathVariable
    String normalizedPid = pid.startsWith("/") ? pid.substring(1) : pid;

    Set<ConstraintViolation<ArchivalRecord>> violations =
        validator.validateValue(ArchivalRecord.class, "pid", normalizedPid);

    if (!violations.isEmpty()) {
      throw new ArchivalRecordInvalidPidException(
          "Cannot reserve archival record. PID does not conform to required format: " + normalizedPid + ". " +  violations
      );
    }

    return archivalRecordService.reserveArchivalRecordByObjectIdAndPid(objectId, normalizedPid);

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

    Set<ConstraintViolation<ArchivalRecord>> violations =
        validator.validateValue(ArchivalRecord.class, "pid", normalizedPid);

    if (!violations.isEmpty()) {
      throw new ArchivalRecordInvalidPidException(
          "Cannot delete archival record. PID does not conform to required format: " + normalizedPid + ". " +  violations
      );
    }

    archivalRecordService.deleteById(normalizedPid);
  }

  @Hidden
  @PatchMapping(path = "/{*pid}") // pattern takes everything after (but includes the leading slash!)
  // TODO open api annotations
  public ArchivalRecord patchArchivalRecord(
      @PathVariable String pid,
      @RequestBody @Valid ArchivalRecordUpdateDto archivalRecordUpdateDto
  ){

    // need to check for the included leading slash from the PathVariable
    String normalizedPid = pid.startsWith("/") ? pid.substring(1) : pid;

    Set<ConstraintViolation<ArchivalRecord>> violations =
        validator.validateValue(ArchivalRecord.class, "pid", normalizedPid);

    if (!violations.isEmpty()) {
      throw new ArchivalRecordInvalidPidException(
          "Cannot patch archival record. Given pid does not conform to required format: " + normalizedPid + ". " +  violations
      );
    }

    var archivalRecordDto = new ArchivalRecordDto();
    archivalRecordDto.setPid(normalizedPid);
    archivalRecordDto.setObjectId(archivalRecordUpdateDto.getObjectId());
    archivalRecordDto.setExternalId(archivalRecordUpdateDto.getExternalId());

    // if no publication date was set -> no parsing needs to be done.
    if(archivalRecordUpdateDto.getPublicationTimeStamp() == null)
      archivalRecordService.saveArchivalRecord(archivalRecordDto);

    Instant publicationDate;
    try {
      publicationDate = Instant.parse(archivalRecordUpdateDto.getPublicationTimeStamp());
      archivalRecordDto.setPublicationTimeStamp(publicationDate);
    } catch (DateTimeParseException e) {
      throw new ArchivalRecordInvalidPublicationTimeStampException(
        "Cannot parse publication timestamp of given archival record to patch: " + archivalRecordUpdateDto.getPublicationTimeStamp() + " " + archivalRecordUpdateDto,
        e
      );
    }

    return archivalRecordService.saveArchivalRecord(archivalRecordDto);

  }

  //TODO openapi
  @PutMapping("/draft/{*pid}")
  public ArchivalRecord draftArchivalRecord(
      @PathVariable String pid,
      @Valid @RequestBody ArchivalRecordDraftDto archivalRecordDraftDto
  ){

    // need to check for the included leading slash from the PathVariable
    String normalizedPid = pid.startsWith("/") ? pid.substring(1) : pid;

    Set<ConstraintViolation<ArchivalRecord>> violations =
        validator.validateValue(ArchivalRecord.class, "pid", normalizedPid);

    if (!violations.isEmpty()) {
      throw new ArchivalRecordInvalidPidException(
          "Cannot draft archival record. Given pid does not conform to required format: " + normalizedPid + ". " +  violations
      );
    }

    return archivalRecordService.draftArchivalRecord(normalizedPid, archivalRecordDraftDto);

  }

  @PutMapping("/published/{*pid}")
  public ArchivalRecord publishArchivalRecord(
      @PathVariable String pid,
      @Valid @RequestBody ArchivalRecordPublishDto archivalRecordPublishDto
  ){

    // need to check for the included leading slash from the PathVariable
    String normalizedPid = pid.startsWith("/") ? pid.substring(1) : pid;

    Set<ConstraintViolation<ArchivalRecord>> violations =
        validator.validateValue(ArchivalRecord.class, "pid", normalizedPid);

    if (!violations.isEmpty()) {
      throw new ArchivalRecordInvalidPidException(
          "Cannot draft archival record. Given pid does not conform to required format: " + normalizedPid + ". " +  violations
      );
    }

    return archivalRecordService.publishArchivalRecord(normalizedPid, archivalRecordPublishDto);

  }

}
