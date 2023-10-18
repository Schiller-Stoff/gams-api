package org.zim.gamsapi.User.interfaces;

import org.springframework.data.repository.CrudRepository;
import org.zim.gamsapi.User.User;

import java.util.Optional;

public interface IUserRepository extends CrudRepository<User, String> {

  Optional<User> findByUsername(String username);

  void deleteUserByUsername(String username);

}
