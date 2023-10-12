package org.zim.gamsapi.User;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.zim.gamsapi.User.exceptions.UserNotFoundException;
import org.zim.gamsapi.User.interfaces.IUserRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

  private final IUserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UserNotFoundException {
    Optional<User> user = userRepository.findByUsername(username);
    if (user.isEmpty()) {
      throw new UserNotFoundException(username);
    }
    return new UserPrincipal(user.get());
  }
}
