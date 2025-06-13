package org.zim.gamsapi.Ingest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.Ingest.exceptions.IngestProcessingException;
import org.zim.gamsapi.Ingest.interfaces.IIngestService;
import org.zim.gamsapi.Ingest.utils.IngestStatics;
import io.swagger.v3.oas.annotations.Hidden;
import org.zim.gamsapi.System.config.OpenAPIConfig;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
@RequestMapping
@Slf4j
@Tag(name = OpenAPIConfig.INGEST_TAG, description = OpenAPIConfig.INGEST_TAG_DESCRIPTION)
public class IngestController {

  private final IIngestService ingestService;

  @PostMapping(
      value = "/api/v1/projects/{projectAbbr}/objects/ingest"
  )
  @Operation(
      summary = "Ingest a zipped bag folder",
      description = "Ingests a zipped BagIt folder containing digital objects and datastreams into the specified project. " +
          "The zipped folder should be provided as a multipart form-data part with the name 'bag'. " +
          "The request must include the project abbreviation in the URL path.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Ingest successful"
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request, e.g. missing or invalid parameters."
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error, e.g. processing failure."
          )
      }
  )
  public void ingest(@ModelAttribute Ingest ingest, HttpServletRequest request) {

    byte[] bagAsZip;
    try {
      Part zipPart = request.getPart(IngestStatics.FORM_PART_NAME.name);
      // null check for the case that the form part is not found
      if(zipPart == null){
        String msg = String.format("No form part with name %s found in multipart request against %s. Got parts: %s", IngestStatics.FORM_PART_NAME.name, request.getRequestURI(), request.getParts());
        log.error(msg);
        throw new IngestProcessingException(msg);
      }

      bagAsZip = zipPart.getInputStream().readAllBytes();
    } catch (IOException e){
      String msg = String.format("Failed to read given zip-file via multipart form-data request for ingest: %s", ingest);
      log.error(msg);
      throw new IngestProcessingException(msg);
    } catch (ServletException e){
      String msg = String.format("Failed to extract form part: %s from multipart request against %s. There might be ", IngestStatics.FORM_PART_NAME.name, request.getRequestURI());
      log.error(msg);
      throw new IngestProcessingException(msg);

    }
    ingest.setZippedBagItFolder(bagAsZip);
    ingestService.ingest(ingest);

    // TODO need to return meaningful information about the ingest (e.g. like a status or a reference to the created object?)
    // return ingest;
  }


}
