package org.ddh.gamsapi.infrastructure.System.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.infrastructure.System.security.exceptions.UserNotAssignedToProjectException;
import org.ddh.gamsapi.infrastructure.System.security.exceptions.UserNotAuthorizedException;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Authorization manager for datastream content access.
 *
 * <p>Authorization hierarchy (first match wins):</p>
 * <ol>
 *   <li>Datastream not found → deny</li>
 *   <li>No content restrictions on datastream → allow (public)</li>
 *   <li>Superadmin (ROLE_admin) → allow</li>
 *   <li>Project admin → allow</li>
 *   <li>Project editor → allow</li>
 *   <li>Project viewer (general) → allow</li>
 *   <li>Content-restricted viewer matching at least one restriction → allow</li>
 *   <li>Otherwise → deny</li>
 * </ol>
 *
 * <p>Keycloak role mapping example for the acceptance criteria:</p>
 * <ul>
 *   <li>Datastream has contentRestriction: {@code "OVER_AGE_18"}</li>
 *   <li>Keycloak role for Max Mustermann: {@code roth_project-viewer_OVER_AGE_18}</li>
 *   <li>Spring authority: {@code ROLE_roth_project-viewer_OVER_AGE_18}</li>
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DatastreamContentAuthorizationManager
    implements AuthorizationManager<RequestAuthorizationContext> {

  private final IDatastreamRepository datastreamRepository;

  private static final AuthorizationDecision GRANTED = new AuthorizationDecision(true);
  private static final AuthorizationDecision DENIED = new AuthorizationDecision(false);

  @Override
  public AuthorizationDecision authorize(
      Supplier<? extends @Nullable Authentication> authenticationSupplier,
      RequestAuthorizationContext context
  ) {
    String projectAbbr = context.getVariables().get("projectAbbr");
    String digitalObjectId = context.getVariables().get("id");
    String dsid = context.getVariables().get("dsid");

    // TODO if dsid is null -> then check if access was being done by main resource

    log.trace("Checking content authorization for {}/{}/{}", projectAbbr, digitalObjectId, dsid);

    // --- 1. Verify datastream exists ---
    // TODO why do i need to check this extra? (already done in service layer)
//    if (!datastreamRepository.existsByDigitalObject_IdAndDsid(digitalObjectId, dsid)) {
//      log.debug("Datastream not found: {}/{} — denying access", digitalObjectId, dsid);
//      return DENIED;
//    }

    // --- 2. Load ONLY content restrictions (lightweight query) ---
    Set<String> contentRestrictions =
        datastreamRepository.findContentRestrictionsByDigitalObjectIdAndDsid(
            digitalObjectId, dsid);

    // --- 3. No restrictions → public access ---
    if (contentRestrictions.isEmpty()) {
      log.trace("No content restrictions on {}/{} — granting public access",
          digitalObjectId, dsid);
      return GRANTED;
    }

    // --- From here, restrictions exist → authentication required ---

    Authentication authentication = authenticationSupplier.get();
    if (authentication == null || !authentication.isAuthenticated()) {
      log.debug("Authentication required for restricted content {}/{}", digitalObjectId, dsid);
      // Return DENIED rather than throwing — let Spring Security handle the
      // 401 redirect via its configured AuthenticationEntryPoint
      return DENIED;
    }

    String username = context.getRequest().getRemoteUser();
    List<String> userAuthorities = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .toList();

    // --- 4. Superadmin bypass ---
    if (userAuthorities.contains(GAMSAPIAuthorities.getAdmin())) {
      log.trace("ACCESS GRANTED — superadmin {} for {}/{}", username, digitalObjectId, dsid);
      return GRANTED;
    }

    // --- 5. Filter to project-relevant roles only ---
    List<String> projectRoles = userAuthorities.stream()
        .filter(role -> GAMSAPIAuthorities.authorityMatchesProjectAbbr(role, projectAbbr))
        .toList();

    if (projectRoles.isEmpty()) {
      throw new UserNotAssignedToProjectException(
          "User " + username + " has no roles for project " + projectAbbr
              + ". Cannot access restricted content. URI: "
              + context.getRequest().getRequestURI());
    }

    // --- 6. Project admin → always allowed ---
    if (projectRoles.contains(GAMSAPIAuthorities.getProjectAdmin(projectAbbr))) {
      log.trace("ACCESS GRANTED — project-admin {} for {}/{}",
          username, digitalObjectId, dsid);
      return GRANTED;
    }

    // --- 7. Project editor → always allowed ---
    if (projectRoles.contains(GAMSAPIAuthorities.getProjectEditor(projectAbbr))) {
      log.trace("ACCESS GRANTED — project-editor {} for {}/{}",
          username, digitalObjectId, dsid);
      return GRANTED;
    }

    // --- 8. General project viewer → always allowed ---
    if (projectRoles.contains(GAMSAPIAuthorities.getProjectViewer(projectAbbr))) {
      log.trace("ACCESS GRANTED — project-viewer {} for {}/{}",
          username, digitalObjectId, dsid);
      return GRANTED;
    }

    // --- 9. Fine-grained content restriction matching ---
    for (String restriction : contentRestrictions) {
      String requiredAuthority =
          GAMSAPIAuthorities.buildProjectViewerContentRestricted(
              projectAbbr, restriction);
      if (userAuthorities.contains(requiredAuthority)) {
        log.trace("ACCESS GRANTED — user {} matched restriction '{}' for {}/{}",
            username, restriction, digitalObjectId, dsid);
        return GRANTED;
      }
    }

    // --- 10. No matching role found ---
    String msg = String.format(
        "User %s lacks required content restriction roles for %s/%s. "
            + "Required one of: %s. User authorities: %s",
        username, digitalObjectId, dsid, contentRestrictions, projectRoles);
    log.warn(msg);
    throw new UserNotAuthorizedException(msg);
  }
}