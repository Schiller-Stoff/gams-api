package org.zim.gamsapi.System.security;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Introduces the /auth endpoint that redirects to the oauth2 login endpoint.
 */
@Controller
public class AuthEndpointController {

  /**
   * Redirects to the oauth2 login endpoint.
   */
  @GetMapping("/api/v1/auth")
  public String login() {
    // TODO careful! - this redirect is not that simple (what if proxy before)?
    return "redirect:/oauth2/authorization/gams-realm";
  }


}
