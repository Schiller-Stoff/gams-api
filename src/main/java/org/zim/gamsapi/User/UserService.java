package org.zim.gamsapi.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.User.exceptions.UserNotFoundException;
import org.zim.gamsapi.User.interfaces.IUserRepository;
import org.zim.gamsapi.User.interfaces.IUserService;
import java.util.ArrayList;
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
  private final PasswordEncoder passwordEncoder;

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

  @Transactional
  public User saveUser(User user){

    Optional<User> foundUserOptional = userRepository.findByUsername(user.getUsername());

    if(foundUserOptional.isEmpty()) {
      user.setPassword(
        passwordEncoder.encode(user.getPassword())
      );
      return userRepository.save(user);
    } else {
      log.info("Found existing user {}. Updating now...", user.getUsername());
    }

    User foundUser = foundUserOptional.get();
    user.setUserid(foundUser.getUserid());

    return userRepository.save(user);
  }

  @Override
  public List<User> findAll() {
    List<User> users = new ArrayList<>();
    userRepository.findAll().forEach(users::add);
    return users;
  }

}
