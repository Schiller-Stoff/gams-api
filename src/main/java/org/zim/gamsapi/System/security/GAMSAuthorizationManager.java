package org.zim.gamsapi.System.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.zim.gamsapi.System.security.exceptions.UserAuthenticationRequiredException;
import org.zim.gamsapi.System.security.exceptions.UserNotAssignedToProjectException;

import java.util.List;
import java.util.function.Supplier;

/**
 * Custom authorization manager for GAMS API.
 * This class is responsible for checking if a user is authorized to access a specific endpoint.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GAMSAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
  @Override
  public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext authorizationContext) {

    log.trace("*** Checking custom authorization process...");
    String requestMethod = authorizationContext.getRequest().getMethod();
    String requestUri = authorizationContext.getRequest().getRequestURI();

    // all GET requests are being authorized
    // TODO not all GET requests should be authorized (e.g. according to rights defined in metadata of a datastream? Means only the content should be blocked?)
//    if(requestMethod.equals(HttpMethod.GET.name())){
//      log.trace("ACCESS GRANTED - GET requests are not protected via the authorization process for url {}", requestUri);
//      return new AuthorizationDecision(true);
//    }

    // all HEAD requests are being authorized
    if(requestMethod.equals(HttpMethod.HEAD.name())){
      log.trace("ACCESS GRANTED - HEAD requests are not protected for url {}", requestUri);
      return new AuthorizationDecision(true);
    }

    // (if user is actually available is already done via authentication workflow (UserDetailsService)
    if(!authentication.get().isAuthenticated()){
      String msg = String.format("User authentication is required for state changing operations on GAMS. Against url %s for method: %s", requestUri, requestMethod);
      log.trace(msg);
      //return new AuthorizationDecision(false);
      throw new UserAuthenticationRequiredException(msg);
    }

    String username = authorizationContext.getRequest().getRemoteUser();
    // access authorities from authentication workflow
    List<String> userAuthorities = authentication.get().getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    // TODO: later add error check if a user has no roles assigned -> therefore no authorities here! (if anynonymous can be checked before)

    // TODO test this
    if(userAuthorities.contains(GAMSAPISecurityRoles.getAnonymous())){
      String msg = String.format("User %s is not authorized for the GAMS-API because having anonymous role: %s. Url: %s Method: %s", GAMSAPISecurityRoles.getAnonymous(), username, requestUri, requestMethod);
      log.trace(msg);
      return new AuthorizationDecision(false);
    }

    // global administrator is allowed to do everything
    if(userAuthorities.contains(GAMSAPISecurityRoles.getAdmin())) {
      log.debug("ACCESS GRANTED for User {} with role '{}' to {} with {}", username, GAMSAPISecurityRoles.ADMINISTRATOR.name, requestUri, requestMethod);
      return new AuthorizationDecision(true);
    }

    // defined in request matcher in SpringSecurityConfiguration.java
    // fail safe nullpointer check (should not happen if request matcher is correctly configured)
    // TODO test this
    String projectAbbr;
    try {
      projectAbbr = authorizationContext.getVariables().get("projectAbbr");
      if(projectAbbr == null) {
        throw new NullPointerException();
      }
    } catch (NullPointerException e) {
      String msg = String.format("Somehow no project abbreviation found in request. The %s class should be only activated at endpoints containing the project-abbreviation. Url: %s Method: %s", this.getClass().getName(), requestUri, requestMethod);
      log.error(msg);
      throw new UserAuthenticationRequiredException(msg);
    }

    // first filter for all project relevant roles
    // TODO test this
    var filteredRoles = userAuthorities.stream()
        .filter(role -> GAMSAPISecurityRoles.authorityMatchesProjectAbbr(role, projectAbbr))
        .toList();

    // there is no role that contains the project-abbreviation ()
    // TODO  test
    if(filteredRoles.isEmpty()) {
      String msg = String.format("User %s is not assigned to project %s. Url: %s Method: %s. Has authorities: %s", username, projectAbbr, requestUri, requestMethod, userAuthorities);
      log.trace(msg);
      throw new UserNotAssignedToProjectException(msg);
    }

    // if project admin - allow everything
    String projectAdminRole = GAMSAPISecurityRoles.getProjectAdmin(projectAbbr);
    if(userAuthorities.contains(projectAdminRole)){
      log.trace("ACCESS GRANTED - User {} is authorized for project {} and has required {} role. Url: {} Method: {}. User authorities: {}", username, projectAbbr, GAMSAPISecurityRoles.PROJECT_ADMINISTRATOR.name, requestUri, requestMethod, userAuthorities);
      return new AuthorizationDecision(true);
    }

    // if project editor (what to allow here?)
    String projectEditorRole = GAMSAPISecurityRoles.getProjectEditor(projectAbbr);
    if(userAuthorities.contains(projectEditorRole)){
      log.debug("ACCESS GRANTED - User {} is authorized for project {} and has required {} role. Url: {} Method: {}. User authorities: {}", username, projectAbbr, GAMSAPISecurityRoles.PROJECT_EDITOR.name, requestUri, requestMethod, userAuthorities);
      return new AuthorizationDecision(true);
    }

    // if project viewer
    // TODO what to allow here - need to think about authorization process
//    String projectViewerRole = GAMSAPISecurityRoles.getProjectViewer(projectAbbr);
//    if(userAuthorities.contains(projectViewerRole)){
//      log.debug("ACCESS GRANTED - User {} is authorized for project {} and has required {} role. Url: {} Method: {}", username, projectAbbr, GAMSAPISecurityRoles.PROJECT_VIEWER.name, requestUri, requestMethod);
//      return new AuthorizationDecision(true);
//    }


    String msg = String.format("No authorization rule applies the current user. User %s is assigned to project %s but has no required role. Url: %s Method: %s.", username, projectAbbr, requestUri, requestMethod);
    log.trace(msg);
    // TODO exception instead?
    return new AuthorizationDecision(false);
  }

}
