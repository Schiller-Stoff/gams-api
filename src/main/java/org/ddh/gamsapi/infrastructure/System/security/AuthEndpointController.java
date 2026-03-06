package org.ddh.gamsapi.infrastructure.System.security;

import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.infrastructure.System.security.exceptions.AuthorizationConfigurationException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles the login entry point for the application.
 * <p>
 * This controller serves as both the login trigger (redirecting to the OAuth2 provider)
 * and the login failure landing page. Spring Security's {@code .loginPage("/api/auth/login")}
 * points here, so unauthenticated users are redirected to this endpoint, and OAuth2 login
 * failures land here with {@code ?error=true}.
 * <p>
 * In normal operation, a GET to {@code /api/auth/login} immediately redirects to the
 * OAuth2 authorization endpoint. When an error parameter is present,
 * the user is redirected to the home page with an error indicator instead of entering
 * an infinite redirect loop back to the failing OAuth2 provider.
 */
@Controller
@Slf4j
public class AuthEndpointController {

  private final ClientRegistrationRepository clientRegistrationRepository;

  public AuthEndpointController(ClientRegistrationRepository clientRegistrationRepository) {
    this.clientRegistrationRepository = clientRegistrationRepository;
  }

  /**
   * Redirects to the OAuth2 login endpoint of the first configured provider,
   * or handles login failure by redirecting to the home page.
   *
   * @param error if present (any value), indicates an OAuth2 login failure.
   *              This prevents a redirect loop back to the failing provider.
   * @return redirect to OAuth2 authorization or to home page on error.
   */
  @GetMapping("/api/auth/login")
  public String login(@RequestParam(value = "error", required = false) String error) {

    // On login failure: redirect to home page instead of re-triggering OAuth2 flow.
    // Without this guard, failureUrl="/api/auth/login?error=true" would redirect right back
    // to the OAuth2 provider, creating an infinite loop.
    if (error != null) {
      log.warn("OAuth2 login failed. Redirecting to home page. Check Keycloak availability and client configuration.");
      return "redirect:/api/v1/?loginError=true";
    }

    String registrationId = null;

    // "Convention over Configuration": Find the first configured provider
    if (clientRegistrationRepository instanceof InMemoryClientRegistrationRepository iterableRepo) {
      for (ClientRegistration reg : iterableRepo) {
        registrationId = reg.getRegistrationId();
        break; // Use the first one found (e.g., "gams-realm")
      }
    }

    if (registrationId == null) {
      throw new AuthorizationConfigurationException(
          "Failed to extract oauth2 client registration id from clientRegistrationRepository."
      );
    }

    // Redirect to the OAuth2 authorization endpoint under /api/auth/
    return "redirect:/api/auth/oauth2/authorization/" + registrationId;
  }
}