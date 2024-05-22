package org.zim.gamsapi.System.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@Slf4j
public class DatastreamContentAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {


  @Override
  public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext object) {

    // TODO think about implementation
    // check in metadata if someone is allowed to access the content!
    // e.g. could restrict a project?

    return new AuthorizationDecision(true);
  }
}
