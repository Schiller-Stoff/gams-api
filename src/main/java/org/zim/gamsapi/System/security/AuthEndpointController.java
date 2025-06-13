package org.zim.gamsapi.System.security;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.zim.gamsapi.System.utils.ControllerUtils;

import java.util.Map;

/**
 * Introduces the /auth endpoint that redirects to the oauth2 login endpoint.
 */
@Controller
public class AuthEndpointController {

  /**
   * Redirects to the oauth2 login endpoint.
   */
  @GetMapping("/api/v1/auth")
  public String login(@RequestHeader Map<String, String> requestHeader) {
    String origin = ControllerUtils.resolveProxiedOrigin(requestHeader);
    // TODO dangerous redirect - hardcoded realm?
    return "redirect:" + origin + "oauth2/authorization/gams-realm";
  }


}
