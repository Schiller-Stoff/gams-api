package org.zim.gamsapi.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.User.interfaces.IUserService;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo userservice. Returns a static testuser.
 * Does not require running external oauth2 resource server.
 * (keycloak)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService implements IUserService {

  @Override
  public User findByUsername(String username) {
    // TODO refactor outdated!
    return new User("", "");
  }

  @Transactional
  public User saveUser(User user){
    // TODO outdated refactor!
    return new User("", "");
  }

  @Override
  public List<User> findAll() {
    // TODO refactor outdated
    return new ArrayList<>();
  }

  @Override
  @Transactional
  public void deleteByUsername(String username) {
    // TODO refactor outdated
  }

}
