package org.ddh.gamsapi.application.Integration.RDF;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.ddh.gamsapi.infrastructure.System.IProxyService;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.ddh.gamsapi.infrastructure.System.configproperties.GAMSDockerDNS;

@Controller
@RequestMapping(value = {"/api/integration/v1/rdf", "/api/integration/v1/rdf/"})
@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = OpenAPIConfig.INTEGRATION_TAG, description = OpenAPIConfig.INTEGRATION_TAG_DESCRIPTION)
public class RDFProxyController {

  private final GAMSDockerDNS gamsConfigProperties;
  private final IProxyService proxyService;

  @Operation(
      summary = "Performs searches via external RDF service",
      description = "This endpoint proxies requests to the RDF service."
  )
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
