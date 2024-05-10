package org.zim.gamsapi.System.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.zim.gamsapi.System.security.GAMSAPISecurityRoles;
import org.zim.gamsapi.System.security.exceptions.UserAssignedToProjectButMissingEditorRoleException;
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
public class Oauth2AuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
  @Override
  public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext authorizationContext) {

    // TODO remove demo logging
    System.out.println("*********AUTH PROCESS");
    log.error("*** AUTHENTICATION PROCESS: ");

    String requestMethod = authorizationContext.getRequest().getMethod();
    String requestUri = authorizationContext.getRequest().getRequestURI();

    log.trace("Checking custom authorization process...");

    // TODO remove demo logging
    log.error("*** AUTHENTICATION: " + authentication);
    log.error("*** AUTH CONTEXT " + authorizationContext);

    // all GET requests are being authorized
    // TODO not all GET requests should be authorized
//    if(requestMethod.equals(HttpMethod.GET.name())){
//      log.trace("ACCESS GRANTED - GET requests are not protected for url {}", requestUri);
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
      return new AuthorizationDecision(false);
      //throw new UserAuthenticationRequiredException(msg);
    }

    String username = authorizationContext.getRequest().getRemoteUser();
    List<String> userAuthorities = authentication.get().getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

    log.error("USERNAME: " + username);

    log.error("USER AUTHORITIES: " + userAuthorities);

    log.error("AUTH CONTEXT VARIABLES: " +  authorizationContext.getVariables());
    //log.error("AUTH CONTEXT VARIABLES: " +  );


//    OAuth2UserAuthority oauth2UserAuthority;
//    try {
//      oauth2UserAuthority = (OAuth2UserAuthority) authentication.get().getAuthorities().toArray()[0];
//    } catch (ClassCastException e){
//      // TODO improve?
//      log.error("CANNOT CAST TO OAUTH2USERAUTHORITY");
//      return new AuthorizationDecision(false);
//    }
//
//    log.error("OAUTH2 AUTHORITY ATTRIBUTES: " + oauth2UserAuthority.getAttributes());
//    log.error("OAUTH2 USER AUTHORITY: " + oauth2UserAuthority.getAuthority());



    // TODO check from here!
    return new AuthorizationDecision(false);





    // TODO from here old code

//
//    String username = authorizationContext.getRequest().getRemoteUser();
//    // access authorities from authentication workflow
//    List<String> userAuthorities = authentication.get().getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
//    // administrator is allowed to do everything
//    if(userAuthorities.contains(GAMSAPISecurityRoles.ADMINISTRATOR.name)) {
//      log.debug("ACCESS GRANTED for User {} with role '{}' to {} with {}", username, GAMSAPISecurityRoles.ADMINISTRATOR.name, requestUri, requestMethod);
//      return new AuthorizationDecision(true);
//    }
//
//    // check if user is assigned to project
//    String projectAbbr = authorizationContext.getVariables().get("projectAbbr"); //defined in request matcher in SpringSecurityConfiguration.java
//    if(userAuthorities.contains(projectAbbr)){
//      // grant access only if assigned project AND editor role.
//      if(userAuthorities.contains(GAMSAPISecurityRoles.EDITOR.name)){
//        log.debug("ACCESS GRANTED - User {} is authorized for project {} and has required {} role. Url: {} Method: {}", username, projectAbbr, GAMSAPISecurityRoles.EDITOR.name, requestUri, requestMethod);
//        return new AuthorizationDecision(true);
//      } else {
//        String msg = String.format("ACCESS DENIED - User %s has access to project %s BUT is missing the required %s role. Url: %s, Method: %s", username, projectAbbr, GAMSAPISecurityRoles.EDITOR.name, requestUri, requestMethod);
//        log.debug(msg);
//        throw new UserAssignedToProjectButMissingEditorRoleException(msg);
//      }
//    } else {
//      String msg = String.format("ACCESS DENIED - User %s is not authorized for project %s. Url: %s, Method: %s", username, projectAbbr, requestUri, requestMethod);
//      log.debug(msg);
//      throw new UserNotAssignedToProjectException(msg);
//    }
  }

}
