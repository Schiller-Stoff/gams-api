package org.ddh.gamsapi.infrastructure.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
@Slf4j
@Tag(name = OpenAPIConfig.USER_TAG, description = OpenAPIConfig.USER_TAG_DESCRIPTION)
public class UserProfileController {

  /**
   * Displays the authenticated user's profile with role assignments.
   * Requires authentication — anonymous users are redirected to login.
   */
  @Operation(
      summary = "Show user info",
      description = "Displays auth information associated with the authenticated user. If the user is not authenticated, they will be redirected to the login page."
  )
  @GetMapping("/api/auth/user")
  public String showUserProfile(Authentication authentication, Model model) {

    // User info from OIDC
    String username = authentication.getName();
    String displayName = username;
    String email = null;

    if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
      displayName = Optional.ofNullable(oidcUser.getFullName()).orElse(username);
      email = oidcUser.getEmail();
    }

    // Parse roles into structured model
    UserRoleProfile roleProfile = new UserRoleProfile(authentication.getAuthorities());

    model.addAttribute("username", username);
    model.addAttribute("displayName", displayName);
    model.addAttribute("email", email);
    model.addAttribute("roleProfile", roleProfile);

    return "auth/profile";
  }
}