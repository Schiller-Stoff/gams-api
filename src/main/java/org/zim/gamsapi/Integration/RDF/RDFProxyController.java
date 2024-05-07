package org.zim.gamsapi.Integration.RDF;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.System.IProxyService;
import org.zim.gamsapi.System.configproperties.GAMSDockerDNS;

@Controller
@RequestMapping(value = {"/api/v1/integration/rdf", "/api/v1/integration/rdf/"})
@Slf4j
@RequiredArgsConstructor
@RestController
public class RDFProxyController {

  private final GAMSDockerDNS gamsConfigProperties;
  private final IProxyService proxyService;

  @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<String> proxy(
          @RequestBody(required = false) String body,
          HttpServletRequest request
  ) {
    log.trace("*** Proxying GET request: {}", request.getRequestURI());
    // Proxy against read only endpoint.
    String targetUrl = String.format("%s/query", gamsConfigProperties.getTriplestoreUrl());
    return proxyService.proxy(request, body, targetUrl);
  }


}
