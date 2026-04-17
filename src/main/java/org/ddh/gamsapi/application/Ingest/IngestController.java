package org.ddh.gamsapi.application.Ingest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Ingest.exceptions.ExportProcessingException;
import org.ddh.gamsapi.application.Ingest.exceptions.IngestProcessingException;
import org.ddh.gamsapi.application.Ingest.interfaces.IIngestService;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectService;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Controller
@RequiredArgsConstructor
@RequestMapping
@Slf4j
@Tag(name = OpenAPIConfig.INGEST_TAG, description = OpenAPIConfig.INGEST_TAG_DESCRIPTION)
public class IngestController {

  private final IIngestService ingestService;
  private final IProjectService projectService;

  @PostMapping(
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = "application/json",
      path = { "/api/curation/v1/projects/{projectAbbr}/objects"}
  )
  @ResponseBody
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
  public void ingest(
      @PathVariable String projectAbbr,
      // TODO remove expected hardcoded form part name
      @RequestParam("subInfoPackZIP") MultipartFile bagFile
  ) {

    // TODO wrong exception
//    if (bagFile.isEmpty()) {
//      throw new IngestProcessingException("Uploaded bag file is empty");
//    }

    // TODO risky validation
    // Validate file size (e.g., max 2GB)
//    long maxSize = 2L * 1024 * 1024 * 1024; // 2GB
//    if (bagFile.getSize() > maxSize) {
//      throw new IngestProcessingException(
//          String.format("Bag file too large: %d bytes (max: %d bytes)",
//              bagFile.getSize(), maxSize)
//      );
//    }

    // TODO i'm not sure if we should validate the content type here - because: clients might also set octect-stream on zip
    // Validate content type
//    String contentType = bagFile.getContentType();
//    if (!"application/zip".equals(contentType) &&
//        !"application/x-zip-compressed".equals(contentType)) {
//      // TODO think about the error here
//      throw new IngestProcessingException(
//          String.format("Invalid content type: %s. Expected application/zip", contentType)
//      );
//    }

    try (InputStream inputStream = bagFile.getInputStream()) {
      ingestService.ingest(projectAbbr, inputStream);
    } catch (IOException e) {
      throw new IngestProcessingException(
          "Failed to read uploaded bag file: " + e.getMessage(),
          e
      );
    }
  }


  @GetMapping(produces = "application/zip", path = { "/api/curation/v1/projects/{projectAbbr}/objects/{id}/export"})
  @ResponseBody
  @Parameter(name = "projectAbbr", description = "Project abbreviation", required = true)
  @Parameter(name = "id", description = "Digital object ID", required = true)
  @Operation(
      summary = "Exports a digital object as a zipped BagIt folder",
      description = "Exports the specified digital object as a zipped BagIt folder. " +
          "The request must include the project abbreviation and the digital object ID in the URL path. " +
          "The response will be a downloadable zip file.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Export successful, returns a zipped BagIt folder."
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
  public void exportBag(@PathVariable String projectAbbr,
                        @PathVariable String id,
                        HttpServletResponse response) {


    log.debug("Export request for object {} in project {}", id, projectAbbr);

    // Verify project exists and matches object
    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);

    // Set response headers
    String filename = id + "-bag.zip";
    response.setContentType("application/zip");
    response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
    // careful when dealing with large files!
    response.setBufferSize(8192);

    try {
      ingestService.exportAsBag(id, response.getOutputStream());
      response.flushBuffer();
    } catch (IOException e) {
      throw new ExportProcessingException(
          "I/O error during bag export for object" + id + "in project" + projectAbbr + ". Original error: " + e.getMessage(),
          e
      );
    }

  }


}
