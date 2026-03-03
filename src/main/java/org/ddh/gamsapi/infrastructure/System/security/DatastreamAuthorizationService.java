package org.ddh.gamsapi.infrastructure.System.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.infrastructure.System.security.exceptions.UserNotAssignedToProjectException;
import org.ddh.gamsapi.infrastructure.System.security.exceptions.UserNotAuthorizedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
@Service
@Slf4j
@RequiredArgsConstructor
public class DatastreamAuthorizationService {

  /**
   * Checks if the given authentication grants access to content
   * with the specified restrictions.
   *
   * @return AuthorizationDecision (granted or denied)
   * @throws UserNotAuthorizedException if user is authenticated but lacks required roles
   * @throws UserNotAssignedToProjectException if user has no roles for the project
   */
  public AuthorizationDecision checkContentAccess(
      String projectAbbr,
      Set<String> contentRestrictions,
      Authentication authentication
  ) {
    // No restrictions → public
    if (contentRestrictions == null || contentRestrictions.isEmpty()) {
      return new AuthorizationDecision(true);
    }

    // Restrictions exist → require authentication
    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return new AuthorizationDecision(false);
    }

    List<String> authorities = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .toList();

    // Superadmin
    if (authorities.contains(GAMSAPIAuthorities.getAdmin())) {
      return new AuthorizationDecision(true);
    }

    // Project-level roles (admin, editor, viewer all see everything)
    if (authorities.contains(GAMSAPIAuthorities.getProjectAdmin(projectAbbr))
        || authorities.contains(GAMSAPIAuthorities.getProjectEditor(projectAbbr))
        || authorities.contains(GAMSAPIAuthorities.getProjectViewer(projectAbbr))) {
      return new AuthorizationDecision(true);
    }

    // Fine-grained content restriction matching
    for (String restriction : contentRestrictions) {
      String required = GAMSAPIAuthorities
          .buildProjectViewerContentRestricted(projectAbbr, restriction);
      if (authorities.contains(required)) {
        return new AuthorizationDecision(true);
      }
    }

    // No match
    throw new UserNotAuthorizedException(
        "User lacks required content restriction roles for project "
            + projectAbbr + ". Required: " + contentRestrictions);
  }
}
