package org.zim.gamsapi.SubInfoPack;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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
  public void ingestSubInfoPack(@ModelAttribute SubInfoPack subInfoPack, @RequestParam MultipartFile subInfoPackZIP){

    byte[] sipAsZIP;
    try {
      sipAsZIP = subInfoPackZIP.getBytes();
    } catch (IOException e){
      String msg = String.format("Cannot retrieve bytes of ingest operation for SIP: %s", subInfoPack);
      log.error(msg);
      throw new SubInfoPackProcessingException(msg);
    }
    subInfoPack.setZippedFolder(sipAsZIP);
    subInfoPackService.ingest(subInfoPack);

    // TODO need to return meaningful information about the submission info-package.
    // return subInfoPack;
  }


}
