package org.ddh.gamsapi.domain.ArchivalRecord;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.dto.ArchivalRecordDraftDto;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.dto.ArchivalRecordDto;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.dto.ArchivalRecordPublishDto;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.dto.ArchivalRecordUpdateDto;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.exceptions.ArchivalRecordInvalidPidException;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.exceptions.ArchivalRecordInvalidPublicationTimeStampException;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.exceptions.ArchivalRecordInvalidStateException;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.ddh.gamsapi.infrastructure.System.dto.PagedResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = { "/api/curation/v1/archival-records" })
@Slf4j
@RequiredArgsConstructor
@Tag(name = OpenAPIConfig.ARCHIVAL_RECORDS_TAG, description = OpenAPIConfig.ARCHIVAL_RECORDS_TAG_DESCRIPTION)
public class ArchivalRecordController {

  private final IArchivalRecordService archivalRecordService;
  private final Validator validator;

  @Operation(
      summary = "Get archival records for a digital object.",
      description = "Retrieve the archival records for a specific digital object.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Successful retrieval of the archival records",
              content = @Content),
          @ApiResponse(responseCode = "404", description = "The associated digital object does not exist.",
              content = @Content)
      }
  )
  @GetMapping
  public PagedResponse<ArchivalRecordCompactView> findArchivalRecords(
      @RequestParam String objectId,
      @RequestParam(defaultValue = "", required = false, name = "state") Set<String> states,
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "100") int pageSize
  ) {

    //  Manually validate and convert the input strings
    Set<ArchivalState> validatedStates = states.stream()
        .filter(s -> !s.isEmpty()) // filter out the default empty string
        .map(s -> {
          try {
            return ArchivalState.valueOf(s.toUpperCase()); // lower case is allowed
          } catch (IllegalArgumentException _) {
            throw new ArchivalRecordInvalidStateException(
                "ArchivalState not known: '" + s + "'"
            );
          }
        })
        .collect(Collectors.toSet());

    if(validatedStates.isEmpty()){
      return archivalRecordService.findForObject(
          objectId,
          PageRequest.of(pageIndex, pageSize, Sort.by("publicationTimeStamp"))
      );
    }

    return archivalRecordService.findArchivalRecordsForObject(
        objectId,
        validatedStates,
        PageRequest.of(pageIndex, pageSize, Sort.by("publicationTimeStamp"))
    );
  }

  @Operation(
      summary = "Get the active archival record for a digital object.",
      description = "Retrieves the currently active archival record for a specific digital object.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Successful retrieval of the active archival record",
              content = @Content),
          @ApiResponse(responseCode = "400", description = "There is no active archival record for given digital object.",
              content = @Content),
          @ApiResponse(responseCode = "500", description = "There are multiple active archival records for the same digital object.",
              content = @Content)
      }
  )
  @GetMapping("/active")
  public PagedResponse<ArchivalRecordCompactView> findActiveArchivalRecord(
      @RequestParam String objectId
  ) {
    return archivalRecordService.findActiveArchivalRecordForObject(objectId);
  }

  @PostMapping
  @Operation(
      summary = "Reserve an archival record for a digital object",
      description = "Allows to create an archival record for a specific digital object by providing the project abbreviation in the path variable, the digital object ID, and the archival record data in the request body.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Archival record successfully created",
              content = @Content),
          @ApiResponse(responseCode = "404", description = "Referenced digital object does not exist.",
              content = @Content),
          @ApiResponse(responseCode = "409", description = "An active archival record for the digital object already exists.",
              content = @Content),
          @ApiResponse(responseCode = "500", description = "Server produced an unexpected (and very rare) pid clash at pid generation.",
              content = @Content)
      }
  )
  public ArchivalRecord reserveArchivalRecordByObjectId(
      @RequestParam String objectId
  ){
    return archivalRecordService.reserveArchivalRecordByObjectId(objectId);
  }

  @Operation(
      summary = "Reserve an archival record for a digital object via providing a pid",
      description = "Allows to create an archival record for a specific digital object by providing the pid.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Archival record successfully created",
              content = @Content),
          @ApiResponse(responseCode = "400", description = "Archival record already exists or validation error.",
              content = @Content),
          @ApiResponse(responseCode = "404", description = "Referenced digital object does not exist.",
              content = @Content),
          @ApiResponse(responseCode = "409", description = "An active archival record for the digital object already exists.",
              content = @Content),
          @ApiResponse(responseCode = "500", description = "Unexpected pid clash on the server.",
              content = @Content)
      }
  )
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

  @DeleteMapping(path = "/{*pid}") // pattern takes everything after (but includes the leading slash!)
  @Operation(
      summary = "Deletes an archival record by it's pid.",
      description = "Allows to delete an archival record by the archival record pid.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Archival record successfully deleted",
              content = @Content),
          @ApiResponse(responseCode = "404", description = "Archival record with given pid does not exist.",
              content = @Content),
          @ApiResponse(responseCode = "500", description = "Unexpected server error.",
              content = @Content),

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

  @PatchMapping(path = "/{*pid}") // pattern takes everything after (but includes the leading slash!)
  @Operation(
      summary = "Updates an archival record.",
      description = "Allows to update an existing archival record. Meant as admin cleanup operation.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Archival record successfully updated.",
              content = @Content),
          @ApiResponse(responseCode = "404", description = "Archival record with given pid does not exist or referenced digital object does not exist.",
              content = @Content),
          @ApiResponse(responseCode = "500", description = "Unexpected server error.",
              content = @Content),

      }
  )
  public ArchivalRecord updateArchivalRecord(
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
    archivalRecordDto.setArchivalState(archivalRecordUpdateDto.getArchivalState());

    // if no publication date was set -> no parsing needs to be done.
    if(archivalRecordUpdateDto.getPublicationTimeStamp() == null)
      archivalRecordService.updateArchivalRecord(archivalRecordDto);

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

    return archivalRecordService.updateArchivalRecord(archivalRecordDto);

  }

  @Operation(
      summary = "Updates a reserved archival record to the draft state.",
      description = "Allows to update a reserved archival record to the draft state via providing the external id of the resource to be archived. Reserves a managed handle.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Archival record successfully drafted.",
              content = @Content),
          @ApiResponse(responseCode = "400", description = "An archival record is already active + validation errors.",
              content = @Content),
          @ApiResponse(responseCode = "404", description = "Archival record with given pid does not exist.",
              content = @Content),
          @ApiResponse(responseCode = "500", description = "Internal handle server communication fails.",
              content = @Content),

      }
  )
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

  @Operation(
      summary = "Updates a reserved archival record to the published state.",
      description = "Allows to update a draft archival record to the published state via providing the publicationTimestamp of the resource to be archived. Provides handles for managed pids.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Archival record successfully published.",
              content = @Content),
          @ApiResponse(responseCode = "400", description = "An archival record is already active + validation errors.",
              content = @Content),
          @ApiResponse(responseCode = "404", description = "Archival record with given pid does not exist.",
              content = @Content),
          @ApiResponse(responseCode = "500", description = "Internal handle server communication fails.",
              content = @Content),

      }
  )
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
