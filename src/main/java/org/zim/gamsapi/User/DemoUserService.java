package org.zim.gamsapi.User;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zim.gamsapi.User.interfaces.IUserService;
import java.util.Arrays;
import java.util.List;

/**
 * Demo userservice. Returns a static testuser.
 * Does not require running external oauth2 resource server.
 * (keycloak)
 */
@Service
@Slf4j
public class DemoUserService implements IUserService {

  private final String[] USER_GROUPS =  { "admin" };

  @Override
  public User getCurrentUser(HttpServletRequest request) {
    User user = new User("12345", "Demo GAMS-API user", USER_GROUPS , "DemoUserService_TestUser");
    log.info("Returning local test user. (Oauth2 configuration is deactivated in current profile)");
    return user;
  }

  @Override
  public List<String> getCurrentUserGroups(HttpServletRequest request) {
    return Arrays.asList(USER_GROUPS);
  }

}
