package org.zim.gamsapi.System.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;
import org.zim.gamsapi.System.security.exceptions.AuthorizationConfigurationException;
import org.zim.gamsapi.System.security.exceptions.UserAuthenticationRequiredException;
import java.util.Optional;

/**
 * Defines how a user principal is being used to create auditing marks in the persistence layer,
 * like CreatedBy or ModifiedBy.
 *
 * https://mayankposts.medium.com/database-auditing-in-spring-boot-with-spring-security-context-and-spring-data-jpa-9215b43744bb
 * https://www.baeldung.com/database-auditing-jpa
 */
@Component
@Slf4j
public class UserPrincipalAuditorMapping implements IUserPrincipalAuditorMapping {
  @Override
  public Optional<String> getCurrentAuditor() {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    // if auth fails
    if((authentication == null) || !authentication.isAuthenticated()){
      String msg = String.format("Tried to map UserPrincipal to Auditor but failed. User seems not to be authenticated so no Auditor can be assigned. Happened in class %s ", this.getClass().getName());
      log.error(msg);
      throw new UserAuthenticationRequiredException(msg);
    }

    try {
      DefaultOidcUser user = (DefaultOidcUser) authentication.getPrincipal();
      // this is the keycloak user-id
      String userName = user.getSubject();
      if (userName == null) {
        String msg = "User subject is null. Failed to extract user's subject from given token. Make sure that you have defined a subject in the keycloak instance.";
        log.error(msg);
        throw new AuthorizationConfigurationException(msg);
      }
      return Optional.of(userName);
    } catch (ClassCastException e){
      String msg = String.format("Failed to extract user principal from given authentication. Mapping from UserPrincipal to Auditor aborted. Is there a valid oauth2 token available? Original error %s", e);
      log.error(msg);
      throw new UserAuthenticationRequiredException(msg);
    }

  }
}
