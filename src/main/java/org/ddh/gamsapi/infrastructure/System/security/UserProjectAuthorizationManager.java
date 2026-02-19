package org.ddh.gamsapi.infrastructure.System.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.infrastructure.System.configproperties.GamsEnvironmentProperties;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.ddh.gamsapi.infrastructure.System.security.exceptions.AuthorizationConfigurationException;
import org.ddh.gamsapi.infrastructure.System.security.exceptions.UserAuthenticationRequiredException;
import org.ddh.gamsapi.infrastructure.System.security.exceptions.UserNotAssignedToProjectException;
import org.ddh.gamsapi.infrastructure.System.security.exceptions.UserNotAuthorizedException;

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

  private final GamsEnvironmentProperties  gamsEnvironmentProperties;

  @Override
  public AuthorizationDecision authorize(Supplier<? extends @Nullable Authentication> authentication, RequestAuthorizationContext authorizationContext) {

    log.trace("*** Checking custom authorization process...");
    String requestMethod = authorizationContext.getRequest().getMethod();
    String requestUri = authorizationContext.getRequest().getRequestURI();

    // small fail check - should not happen if request matcher is correctly configured (in spring configuration)
    if(!requestUri.contains("/projects/")){
      throw new AuthorizationConfigurationException(
          "The authorization manager " + this.getClass().getName() + " was activated for a request that does not contain '/projects/' in the url. This should not happen. Url: " + requestUri + " Method: " + requestMethod
      );
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
      String msg = "User is not authenticated for state changing operations on GAMS. Against url " + requestUri + " for method: " + requestMethod;
      log.debug(msg);
      throw new AccessDeniedException(msg);
    }

    //TODO test
    if(authorizationContext.getRequest().getSession() == null){
      String msg = "User session is required for state changing operations on GAMS. Against url " + requestUri + " for method: " + requestMethod;
      log.debug(msg);
      throw new UserAuthenticationRequiredException(msg);
    }

    // TODO I think this remote user is quite unreliable? - check
    String username = authorizationContext.getRequest().getRemoteUser();
    // failsafe if username is unexpectedly null
    // TODO test
    if(username == null){
      // TODO improve error message
      String msg = "Remote user is unexpectedly null. This should not happen. Url: " + requestUri + " Method: " + requestMethod;
      log.error(msg);
      throw new AccessDeniedException(msg);
    }


    // access authorities from authentication workflow
    List<String> userAuthorities = authentication.get().getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

    if(userAuthorities.contains(GAMSAPIAuthorities.getAnonymous())){
      String msg = "User with name " + username + " is not authorized for state changing operations on the GAMS-API because having anonymous role: " + GAMSAPIAuthorities.getAnonymous() + ". Url: " + requestUri + " Method: " + requestMethod;
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
      String msg = "Somehow no project abbreviation found in request. The " + this.getClass().getName() + " class should be only activated at endpoints containing the project-abbreviation. Url: " + requestUri + " Method: " + requestMethod + " Original error: " + e.getMessage();
      log.error(msg);
      throw new AuthorizationConfigurationException(
          msg,
          e
      );
    }

    // first filter for all project relevant roles
    var filteredRoles = userAuthorities.stream()
        .filter(role -> GAMSAPIAuthorities.authorityMatchesProjectAbbr(role, projectAbbr))
        .toList();

    // there is no role that contains the project-abbreviation ()
    if(filteredRoles.isEmpty()) {
      String msg = "User" + username + " is not assigned to project " + projectAbbr + ". Url: " + requestUri + " Method: " + requestMethod + ". Has authorities: " + userAuthorities;
      log.debug(msg);
      throw new UserNotAssignedToProjectException(msg);
    }

    // if project admin - allow everything
    String projectAdminRole = GAMSAPIAuthorities.getProjectAdmin(projectAbbr);
    if(userAuthorities.contains(projectAdminRole)){
      log.trace("ACCESS GRANTED - User {} is authorized for project {} and has required {} role. Url: {} Method: {}. User authorities: {}", username, projectAbbr, GAMSAPIAuthorities.PROJECT_ADMINISTRATOR.name, requestUri, requestMethod, userAuthorities);
      return new AuthorizationDecision(true);
    }

    // if project editor

    String projectEditorRole = GAMSAPIAuthorities.getProjectEditor(projectAbbr);
    if(userAuthorities.contains(projectEditorRole)){
      log.debug("ACCESS GRANTED - User {} is authorized for project {} and has required {} role. Url: {} Method: {}. User authorities: {}", username, projectAbbr, GAMSAPIAuthorities.PROJECT_EDITOR.name, requestUri, requestMethod, userAuthorities);

      if(gamsEnvironmentProperties.isAllowDirectModifications()) return new AuthorizationDecision(true);

      if(requestMethod.equals(HttpMethod.PATCH.name()) || requestMethod.equals(HttpMethod.PUT.name())){
        String msg = "Project editor must not change project data when property 'allow-direct-modifications' is false. Url: ' " + requestUri + "' Method: '" + requestMethod + "'";
        log.trace(msg);
        throw new UserNotAuthorizedException(msg);
      }

      if( requestUri.contains("/datastreams/") && requestMethod.equals(HttpMethod.DELETE.name())) {
        String msg = "Project editor must not delete a singular datastream when property 'allow-direct-modifications' is false. Url: ' " + requestUri + "' Method: '" + requestMethod + "'";
        log.trace(msg);
        throw new UserNotAuthorizedException(msg);
      }

      return new AuthorizationDecision(true);

    }

    // if project viewer
    // TODO what to allow here - need to think about authorization process
//    String projectViewerRole = GAMSAPISecurityRoles.getProjectViewer(projectAbbr);
//    if(userAuthorities.contains(projectViewerRole)){
//      log.debug("ACCESS GRANTED - User {} is authorized for project {} and has required {} role. Url: {} Method: {}", username, projectAbbr, GAMSAPISecurityRoles.PROJECT_VIEWER.name, requestUri, requestMethod);
//      return new AuthorizationDecision(true);
//    }


    log.trace("No authorization rule unexpectedly applies the current user. User {} is assigned to project {} but has no required role. Url: {} Method: {}.", username, projectAbbr, requestUri, requestMethod);
    // TODO exception instead?
    return new AuthorizationDecision(false);
  }

}
