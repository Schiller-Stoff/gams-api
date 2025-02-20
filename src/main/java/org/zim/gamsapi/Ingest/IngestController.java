package org.zim.gamsapi.Ingest;

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

import java.io.IOException;

@Controller
@RequiredArgsConstructor
@Hidden
@RequestMapping
@Slf4j
public class IngestController {

  private final IIngestService ingestService;

  @PostMapping(produces = "application/json", path = {"/api/v1/projects/{projectAbbr}/objects/", "/api/v1/projects/{projectAbbr}/objects"})
  @ResponseBody
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
