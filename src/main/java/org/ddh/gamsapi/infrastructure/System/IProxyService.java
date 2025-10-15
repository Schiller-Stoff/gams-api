package org.ddh.gamsapi.infrastructure.System;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

/**
 * Used to proxy_pass (nginx style) requests to specified locations
 */
public interface IProxyService {

  /**
   * Proxies given request to given target location = url.
   * @param request Spring boot HttpServletRequest object.
   * @param body Body of the source request as string.
   * @param targetUrl Url to which should be proxied e.g. http://myService:8080/api/v10/
   * @return Proxied response to be returned by proxy() caller.
   */
  ResponseEntity<String> proxy(HttpServletRequest request, String body, String targetUrl);

}

