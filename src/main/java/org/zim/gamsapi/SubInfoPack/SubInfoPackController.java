package org.zim.gamsapi.SubInfoPack;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.SubInfoPack.exceptions.SubInfoPackProcessingException;
import org.zim.gamsapi.SubInfoPack.interfaces.ISubInfoPackService;
import java.io.IOException;

@Controller
@RequiredArgsConstructor
@RequestMapping
@Slf4j
public class SubInfoPackController {

  private final ISubInfoPackService subInfoPackService;

  @PostMapping(produces = "application/json", path = {"/api/v1/projects/{projectAbbr}/objects/", "/api/v1/projects/{projectAbbr}/objects"})
  @ResponseBody
  public void ingestSubInfoPack(@ModelAttribute SubInfoPack subInfoPack, HttpServletRequest request) {

    byte[] sipAsZIP;
    final String ZIP_FORM_PART_NAME = "subInfoPackZIP";
    try {
      Part subInfoPackZIPPart = request.getPart(ZIP_FORM_PART_NAME);
      sipAsZIP = subInfoPackZIPPart.getInputStream().readAllBytes();
    } catch (IOException e){
      String msg = String.format("Failed to read given zip-file via multipart form-data request for subInfoPack: %s", subInfoPack);
      log.error(msg);
      throw new SubInfoPackProcessingException(msg);
    } catch (ServletException e){
      String msg = String.format("Failed to extract form part: %s from multipart request against %s. There might be ", ZIP_FORM_PART_NAME, request.getRequestURI());
      log.error(msg);
      throw new SubInfoPackProcessingException(msg);

    }
    subInfoPack.setZippedFolder(sipAsZIP);
    subInfoPackService.ingest(subInfoPack);

    // TODO need to return meaningful information about the submission info-package.
    // return subInfoPack;
  }


}
