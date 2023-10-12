package org.zim.gamsapi.User;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zim.gamsapi.User.exceptions.UserNotFoundException;
import org.zim.gamsapi.User.interfaces.IUserRepository;
import org.zim.gamsapi.User.interfaces.IUserService;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Demo userservice. Returns a static testuser.
 * Does not require running external oauth2 resource server.
 * (keycloak)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService implements IUserService {

  private final IUserRepository userRepository;

  @Override
  public User findByUsername(String username) {

    Optional<User> foundUser = userRepository.findByUsername(username);
    if(foundUser.isEmpty()){
      String msg = String.format("Cannot find user with name %s.", username);
      log.error(msg);
      throw new UserNotFoundException(msg);
    }

    log.info("Found user {}", foundUser);

    return foundUser.get();
  }

}
