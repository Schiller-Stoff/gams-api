package org.ddh.gamsapi.infrastructure.User;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
@Slf4j
public class UserProfileController {

  /**
   * Displays the authenticated user's profile with role assignments.
   * Requires authentication — anonymous users are redirected to login.
   */
  @GetMapping("/api/auth/profile")
  public String showUserProfile(Authentication authentication, Model model) {

    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return "redirect:/api/v1/auth";
    }

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