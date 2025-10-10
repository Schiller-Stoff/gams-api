package org.zim.gamsapi.System.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.DatastreamId;
import org.zim.gamsapi.Datastream.utils.interfaces.IDatastreamRepository;
import org.zim.gamsapi.Datastream.utils.exceptions.DatastreamNotFoundException;
import org.zim.gamsapi.System.security.exceptions.UserNotAssignedToProjectException;
import org.zim.gamsapi.System.security.exceptions.UserNotAuthorizedException;
import java.util.List;
import java.util.function.Supplier;

@Component
@Slf4j
@RequiredArgsConstructor
public class DatastreamContentAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

  final IDatastreamRepository datastreamRepository;


  @Override
  public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext authorizationContext) {

    // TODO write tests!

    // TODO throw error if not a GET request (should be configured outside of this class)

    // TODO check if any of these value is null?
    String projectAbbr = authorizationContext.getVariables().get("projectAbbr");
    String digitalObjectId = authorizationContext.getVariables().get("id");
    String dsid = authorizationContext.getVariables().get("dsid");

    
    log.trace("Checking custom authorization process for datastream content {}", dsid);

    Datastream datastream = datastreamRepository.findById(
        DatastreamId.builder()
            .digitalObject(digitalObjectId)
            .dsid(dsid)
            .build()
    ).orElseThrow(() -> {
      String msg = String.format("Authorization missing datastream metadata: Requested datastream %s not found for digital object %s for project: %s. At url: %s", dsid, digitalObjectId, projectAbbr, authorizationContext.getRequest().getRequestURI());
      log.info(msg);
      return new DatastreamNotFoundException(msg);
    });

    // without content restrictions, always allow access
    if(datastream.getContentRestrictions().isEmpty()){
      return new AuthorizationDecision(true);
    }

    if(!authentication.get().isAuthenticated()){
      String msg = String.format("User authentication is required displaying the datastream content. Against url %s for method: %s", authorizationContext.getRequest().getRequestURI(), authorizationContext.getRequest().getMethod());
      log.error(msg);
      throw new AccessDeniedException(msg);
    }

    String username = authorizationContext.getRequest().getRemoteUser();
    if(username == null){
      // TODO improve log message
      String msg = String.format("Remote user is unexpectedly null. This should not happen. Url: %s Method: %s", authorizationContext.getRequest().getRequestURI(), authorizationContext.getRequest().getMethod());
      log.error(msg);
      throw new AccessDeniedException(msg);
    }

    List<String> userAuthorities = authentication.get().getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    if(userAuthorities.contains(GAMSAPIAuthorities.getAnonymous())){
      // TODO improve log mewsage
      String msg = String.format("User with name %s is not authorized for state changing operations on the GAMS-API because having anonymous role: %s. Url: %s Method: %s", username, GAMSAPIAuthorities.getAnonymous(), authorizationContext.getRequest().getRequestURI(), authorizationContext.getRequest().getMethod());
      log.trace(msg);
      throw new UserNotAuthorizedException(msg);
    }

    // TODO add: superadmin always allowed to see content


    // first filter for all project relevant roles
    var filteredRoles = userAuthorities.stream()
        .filter(role -> GAMSAPIAuthorities.authorityMatchesProjectAbbr(role, projectAbbr))
        .toList();

    if(filteredRoles.isEmpty()) {
      // TODO better message
      String msg = String.format("User %s is not assigned to project %s. And therefore not allowed to see the requested datastream content. Url: %s Method: %s. Has authorities: %s", username, projectAbbr, authorizationContext.getRequest().getRequestURI(), authorizationContext.getRequest().getMethod(), userAuthorities);
      log.trace(msg);
      throw new UserNotAssignedToProjectException(msg);
    }

    // project editor can ALWAYS see project contents
    String projectEditorRole = GAMSAPIAuthorities.getProjectEditor(projectAbbr);
    if(userAuthorities.contains(projectEditorRole)){
      //TODO better message
      log.trace("ACCESS GRANTED for User {} with role '{}' to {} with {}", username, projectEditorRole, authorizationContext.getRequest().getRequestURI(), authorizationContext.getRequest().getMethod());
      return new AuthorizationDecision(true);
    }

    // general project viewer can always see every datastream content
    String projectViewerRole = GAMSAPIAuthorities.getProjectViewer(projectAbbr);
    if(userAuthorities.contains(projectViewerRole)){
      //TODO better message
      log.trace("ACCESS GRANTED for User {} with role '{}' to {} with {}", username, projectViewerRole, authorizationContext.getRequest().getRequestURI(), authorizationContext.getRequest().getMethod());
      return new AuthorizationDecision(true);
    }

    // fine grained control
    for (String contentRestriction : datastream.getContentRestrictions()) {
      // TODO validate somehow?
      String contentRestrictionRole = GAMSAPIAuthorities.buildProjectViewerContentRestricted(projectAbbr, contentRestriction);
      if(userAuthorities.contains(contentRestrictionRole)){
        log.trace("ACCESS GRANTED for User {} with role '{}' to {} with {}", username, contentRestriction, authorizationContext.getRequest().getRequestURI(), authorizationContext.getRequest().getMethod());
        return new AuthorizationDecision(true);
      }
    }

    // TODO better message
    String msg = String.format("User %s is not allowed to see the requested datastream content. The user is missing the required roles. Url: %s Method: %s. Has authorities: %s", username, authorizationContext.getRequest().getRequestURI(), authorizationContext.getRequest().getMethod(), userAuthorities);
    log.error(msg);
    throw new UserNotAuthorizedException(msg);


    //  TODO this GET restriction on datastream content -> will this require logout controlled via webclient?

  }
}
