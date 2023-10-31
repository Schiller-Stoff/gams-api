package org.zim.gamsapi.Integration.BaseSearch;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.Integration.Common.interfaces.IIntegrationProxyController;
import org.zim.gamsapi.System.IProxyService;
import org.zim.gamsapi.System.configproperties.GAMSDockerDNS;

/**
 * Routes incoming search request to dedicated GAMS search service.
 */
@Controller
@RequestMapping(value = {"/api/v1/integration/search", "/api/v1/integration/search/"})
@Slf4j
@RequiredArgsConstructor
@RestController
public class BaseSearchProxyController implements IIntegrationProxyController {

  private final IProxyService proxyService;
  private final GAMSDockerDNS gamsConfigProperties;

  @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<String> proxy(
          @RequestBody(required = false) String body,
          HttpServletRequest request
  ) {
    log.trace("*** Proxying request: {}", request.getRequestURI());
    // proxy against read only endpoint.
    String targetUrl = String.format("%s/select", gamsConfigProperties.getFacetSearchUrl());
    return proxyService.proxy(request, body, targetUrl);
  }
}
