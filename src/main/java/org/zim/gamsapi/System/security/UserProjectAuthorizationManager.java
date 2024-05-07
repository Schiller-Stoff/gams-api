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
import org.zim.gamsapi.System.security.exceptions.UserAssignedToProjectButMissingEditorRoleException;
import org.zim.gamsapi.System.security.exceptions.UserAuthenticationRequiredException;
import org.zim.gamsapi.System.security.exceptions.UserNotAssignedToProjectException;
import java.util.List;
import java.util.function.Supplier;


/**
 * Handles authorization for the gams-api.
 * Like a user must have required role and assignment to project to be able to
 * change state of digital objects and datastreams.
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class UserProjectAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

  @Override
  public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext authorizationContext) {

    String requestMethod = authorizationContext.getRequest().getMethod();
    String requestUri = authorizationContext.getRequest().getRequestURI();

    log.trace("Checking custom authorization process...");

    // all GET requests are being authorized
    if(requestMethod.equals(HttpMethod.GET.name())){
      log.trace("ACCESS GRANTED - GET requests are not protected for url {}", requestUri);
      return new AuthorizationDecision(true);
    }

    // all HEAD requests are being authorized
    if(requestMethod.equals(HttpMethod.HEAD.name())){
      log.trace("ACCESS GRANTED - HEAD requests are not protected for url {}", requestUri);
      return new AuthorizationDecision(true);
    }

    // (if user is actually available is already done via authentication workflow (UserDetailsService)
    if(!authentication.get().isAuthenticated()){
      String msg = String.format("User authentication is required for state changing operations on GAMS. Against url %s for method: %s", requestUri, requestMethod);
      log.trace(msg);
      throw new UserAuthenticationRequiredException(msg);
    }

    String username = authorizationContext.getRequest().getRemoteUser();
    // access authorities from authentication workflow
    List<String> userAuthorities = authentication.get().getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    // administrator is allowed to do everything
    if(userAuthorities.contains(GAMSAPISecurityRoles.ADMINISTRATOR.name)) {
      log.debug("ACCESS GRANTED for User {} with role '{}' to {} with {}", username, GAMSAPISecurityRoles.ADMINISTRATOR.name, requestUri, requestMethod);
      return new AuthorizationDecision(true);
    }

    // check if user is assigned to project
    String projectAbbr = authorizationContext.getVariables().get("projectAbbr"); //defined in request matcher in SpringSecurityConfiguration.java
    if(userAuthorities.contains(projectAbbr)){
      // grant access only if assigned project AND editor role.
      if(userAuthorities.contains(GAMSAPISecurityRoles.EDITOR.name)){
        log.debug("ACCESS GRANTED - User {} is authorized for project {} and has required {} role. Url: {} Method: {}", username, projectAbbr, GAMSAPISecurityRoles.EDITOR.name, requestUri, requestMethod);
        return new AuthorizationDecision(true);
      } else {
        String msg = String.format("ACCESS DENIED - User %s has access to project %s BUT is missing the required %s role. Url: %s, Method: %s", username, projectAbbr, GAMSAPISecurityRoles.EDITOR.name, requestUri, requestMethod);
        log.debug(msg);
        throw new UserAssignedToProjectButMissingEditorRoleException(msg);
      }
    } else {
      String msg = String.format("ACCESS DENIED - User %s is not authorized for project %s. Url: %s, Method: %s", username, projectAbbr, requestUri, requestMethod);
      log.debug(msg);
      throw new UserNotAssignedToProjectException(msg);
    }
  }

}
