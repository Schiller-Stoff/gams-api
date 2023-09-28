package org.zim.gamsapi.Integration.Facet;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zim.gamsintegrationapi.System.GAMSConfigProperties;
import org.zim.gamsintegrationapi.System.IProxyService;

/**
 * Routes incoming search request to dedicated GAMS search service.
 */
@Controller
@RequestMapping(value = {"/api/v1/integration/search", "/api/v1/integration/search/"})
@Slf4j
@RequiredArgsConstructor
@RestController
public class SearchController {

  private final IProxyService proxyService;
  private final GAMSConfigProperties gamsConfigProperties;

  @GetMapping("/**")
  public ResponseEntity<String> proxySolr(
          @RequestBody(required = false) String body,
          HttpServletRequest request
  ) {
    log.trace("*** Proxying request: {}", request.getRequestURI());
    String targetUrl = String.format("%s/select", gamsConfigProperties.getFacetSearchUrl());
    return proxyService.proxy(request, body, targetUrl);
  }
}
