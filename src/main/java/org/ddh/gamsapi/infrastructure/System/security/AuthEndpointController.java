package org.ddh.gamsapi.infrastructure.System.security;

import org.ddh.gamsapi.infrastructure.System.security.exceptions.AuthorizationConfigurationException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthEndpointController {

  private final ClientRegistrationRepository clientRegistrationRepository;

  public AuthEndpointController(ClientRegistrationRepository clientRegistrationRepository) {
    this.clientRegistrationRepository = clientRegistrationRepository;
  }

  /**
   * Redirects to the oauth2 login endpoint of the first configured provider.
   */
  @GetMapping("/api/v1/auth")
  public String login() {
    String registrationId = null;

    // "Convention over Configuration": Find the first configured provider
    if (clientRegistrationRepository instanceof InMemoryClientRegistrationRepository iterableRepo) {
      for (ClientRegistration reg : iterableRepo) {
        registrationId = reg.getRegistrationId();
        break; // Use the first one found (e.g., "gams-realm")
      }
    }

    // Fallback if repository is not iterable or empty (should ideally throw an error)
    if (registrationId == null) {
      throw new AuthorizationConfigurationException(
          "Failed to extract oauth2 client registration id from clientRegistrationRepository."
      );
    }

    return "redirect:/oauth2/authorization/" + registrationId;
  }
}