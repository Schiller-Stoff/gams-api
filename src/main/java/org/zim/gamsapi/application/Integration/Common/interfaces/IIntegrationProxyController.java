package org.zim.gamsapi.application.Integration.Common.interfaces;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

/**
 * Proxies GET requests to connected services.
 * Defines under which request-url the proxying should take place.
 */
public interface IIntegrationProxyController {

  ResponseEntity<String> proxy(String body, HttpServletRequest request);

}
