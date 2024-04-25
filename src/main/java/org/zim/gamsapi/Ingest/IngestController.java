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
import java.io.IOException;

@Controller
@RequiredArgsConstructor
@RequestMapping
@Slf4j
public class IngestController {

  private final IIngestService ingestService;

  @PostMapping(produces = "application/json", path = {"/api/v1/projects/{projectAbbr}/objects/", "/api/v1/projects/{projectAbbr}/objects"})
  @ResponseBody
  public void ingest(@ModelAttribute Ingest ingest, HttpServletRequest request) {

    byte[] bagAsZip;
    // TODO this form part name needs to be updated!
    final String ZIP_FORM_PART_NAME = "subInfoPackZIP";
    try {
      Part zipPart = request.getPart(ZIP_FORM_PART_NAME);
      bagAsZip = zipPart.getInputStream().readAllBytes();
    } catch (IOException e){
      String msg = String.format("Failed to read given zip-file via multipart form-data request for ingest: %s", ingest);
      log.error(msg);
      throw new IngestProcessingException(msg);
    } catch (ServletException e){
      String msg = String.format("Failed to extract form part: %s from multipart request against %s. There might be ", ZIP_FORM_PART_NAME, request.getRequestURI());
      log.error(msg);
      throw new IngestProcessingException(msg);

    }
    ingest.setZippedBagItFolder(bagAsZip);
    ingestService.ingest(ingest);

    // TODO need to return meaningful information about the ingest.
    // return ingest;
  }


}
