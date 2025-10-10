package org.zim.gamsapi.infrastructure.System.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.zim.gamsapi.infrastructure.System.security.exceptions.AuthorizationConfigurationException;
import org.zim.gamsapi.infrastructure.System.security.exceptions.UserAuthenticationRequiredException;
import org.zim.gamsapi.infrastructure.System.security.exceptions.UserNotAssignedToProjectException;
import org.zim.gamsapi.infrastructure.System.security.exceptions.UserNotAuthorizedException;

import java.util.List;
import java.util.function.Supplier;

/**
 * Custom authorization manager for GAMS API.
 * This class is responsible for checking if a user is authorized to access a specific endpoint according to
 * his project roles (and if super admin)
 * In sum: state changing operations are only allowed for users that are assigned to the project and have the required role.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserProjectAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
  @Override
  public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext authorizationContext) {

    log.trace("*** Checking custom authorization process...");
    String requestMethod = authorizationContext.getRequest().getMethod();
    String requestUri = authorizationContext.getRequest().getRequestURI();

    // small fail check - should not happen if request matcher is correctly configured (in spring configuration)
    if(!requestUri.contains("/projects/")){
      String msg = String.format("No '/projects/' found in request url. The %s class should be only activated at endpoints containing the project-abbreviation in the url, like '/api/v1/projects/demo/objects/demo'. Url: %s Method: %s", this.getClass().getName(), requestUri, requestMethod);
      log.error(msg);
      throw new AuthorizationConfigurationException(msg);
    }

    // all GET requests are being authorized (when urls pattern matches)
    if(requestMethod.equals(HttpMethod.GET.name())){
      log.trace("ACCESS GRANTED - GET requests are not protected via the authorization process for url {}", requestUri);
      return new AuthorizationDecision(true);
    }

    // all HEAD requests are being authorized
    if(requestMethod.equals(HttpMethod.HEAD.name())){
      log.trace("ACCESS GRANTED - HEAD requests are not protected for url {}", requestUri);
      return new AuthorizationDecision(true);
    }

    if(!authentication.get().isAuthenticated()){
      String msg = String.format("User authentication is required for state changing operations on GAMS. Against url %s for method: %s", requestUri, requestMethod);
      log.debug(msg);
      throw new AccessDeniedException(msg);
    }

    //TODO test
    if(authorizationContext.getRequest().getSession() == null){
      String msg = String.format("User session is required for state changing operations on GAMS. Against url %s for method: %s", requestUri, requestMethod);
      log.debug(msg);
      throw new UserAuthenticationRequiredException(msg);
    }

    // TODO I think this remote user is quite unreliable? - check
    String username = authorizationContext.getRequest().getRemoteUser();
    // failsafe if username is unexpectedly null
    // TODO test
    if(username == null){
      // TODO improve error message
      String msg = String.format("Remote user is unexpectedly null. This should not happen. Url: %s Method: %s", requestUri, requestMethod);
      log.error(msg);
      throw new AccessDeniedException(msg);
    }


    // access authorities from authentication workflow
    List<String> userAuthorities = authentication.get().getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

    if(userAuthorities.contains(GAMSAPIAuthorities.getAnonymous())){
      String msg = String.format("User with name %s is not authorized for state changing operations on the GAMS-API because having anonymous role: %s. Url: %s Method: %s", username, GAMSAPIAuthorities.getAnonymous(), requestUri, requestMethod);
      log.debug(msg);
      throw new UserNotAuthorizedException(msg);
    }

    // global administrator is allowed to do everything
    if(userAuthorities.contains(GAMSAPIAuthorities.getAdmin())) {
      log.debug("ACCESS GRANTED for User {} with role '{}' to {} with {}", username, GAMSAPIAuthorities.ADMINISTRATOR.name, requestUri, requestMethod);
      return new AuthorizationDecision(true);
    }

    // defined in request matcher in SpringSecurityConfiguration.java
    // fail safe nullpointer check (should not happen if request matcher is correctly configured)
    String projectAbbr;
    try {
      projectAbbr = authorizationContext.getVariables().get("projectAbbr");
      if(projectAbbr == null) {
        throw new NullPointerException();
      }
    } catch (NullPointerException e) {
      String msg = String.format("Somehow no project abbreviation found in request. The %s class should be only activated at endpoints containing the project-abbreviation. Url: %s Method: %s", this.getClass().getName(), requestUri, requestMethod);
      log.error(msg);
      throw new AuthorizationConfigurationException(msg);
    }

    // first filter for all project relevant roles
    var filteredRoles = userAuthorities.stream()
        .filter(role -> GAMSAPIAuthorities.authorityMatchesProjectAbbr(role, projectAbbr))
        .toList();

    // there is no role that contains the project-abbreviation ()
    if(filteredRoles.isEmpty()) {
      String msg = String.format("User %s is not assigned to project %s. Url: %s Method: %s. Has authorities: %s", username, projectAbbr, requestUri, requestMethod, userAuthorities);
      log.debug(msg);
      throw new UserNotAssignedToProjectException(msg);
    }

    // if project admin - allow everything
    String projectAdminRole = GAMSAPIAuthorities.getProjectAdmin(projectAbbr);
    if(userAuthorities.contains(projectAdminRole)){
      log.trace("ACCESS GRANTED - User {} is authorized for project {} and has required {} role. Url: {} Method: {}. User authorities: {}", username, projectAbbr, GAMSAPIAuthorities.PROJECT_ADMINISTRATOR.name, requestUri, requestMethod, userAuthorities);
      return new AuthorizationDecision(true);
    }

    // if project editor (what to allow here?)
    String projectEditorRole = GAMSAPIAuthorities.getProjectEditor(projectAbbr);
    if(userAuthorities.contains(projectEditorRole)){
      log.debug("ACCESS GRANTED - User {} is authorized for project {} and has required {} role. Url: {} Method: {}. User authorities: {}", username, projectAbbr, GAMSAPIAuthorities.PROJECT_EDITOR.name, requestUri, requestMethod, userAuthorities);
      return new AuthorizationDecision(true);
    }

    // if project viewer
    // TODO what to allow here - need to think about authorization process
//    String projectViewerRole = GAMSAPISecurityRoles.getProjectViewer(projectAbbr);
//    if(userAuthorities.contains(projectViewerRole)){
//      log.debug("ACCESS GRANTED - User {} is authorized for project {} and has required {} role. Url: {} Method: {}", username, projectAbbr, GAMSAPISecurityRoles.PROJECT_VIEWER.name, requestUri, requestMethod);
//      return new AuthorizationDecision(true);
//    }


    String msg = String.format("No authorization rule unexpectedly applies the current user. User %s is assigned to project %s but has no required role. Url: %s Method: %s.", username, projectAbbr, requestUri, requestMethod);
    log.trace(msg);
    // TODO exception instead?
    return new AuthorizationDecision(false);
  }

}
