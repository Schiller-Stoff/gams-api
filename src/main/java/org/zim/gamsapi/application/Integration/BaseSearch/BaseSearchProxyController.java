package org.zim.gamsapi.application.Integration.BaseSearch;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.application.Integration.Common.interfaces.IIntegrationProxyController;
import org.zim.gamsapi.infrastructure.System.IProxyService;
import org.zim.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.zim.gamsapi.infrastructure.System.configproperties.GAMSDockerDNS;

/**
 * Routes incoming search request to dedicated GAMS search service.
 */
@Controller
// TODO should expose "baseSearch" in url
@RequestMapping(value = {"/api/v1/integration/search" })
@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = OpenAPIConfig.INTEGRATION_TAG, description = OpenAPIConfig.INTEGRATION_TAG_DESCRIPTION)
public class BaseSearchProxyController implements IIntegrationProxyController {

  private final IProxyService proxyService;
  private final GAMSDockerDNS gamsConfigProperties;

  @Operation(
      summary = "Perform searches via Base Search service",
      description = "This endpoint proxies requests to the Base Search service."
  )
  @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<String> proxy(
          @RequestBody(required = false) String body,
          HttpServletRequest request
  ) {
    log.trace("*** Proxying request: {}", request.getRequestURI());
    // proxy against read only endpoint.
    // TODO should expose "baseSearch" in url
    // TOD refactor
    String targetUrl = gamsConfigProperties.getBaseSearchUrl() + "/" +  request.getRequestURI().replace("/api/v1/integration/search/", "");
    return proxyService.proxy(request, body, targetUrl);
  }
}
