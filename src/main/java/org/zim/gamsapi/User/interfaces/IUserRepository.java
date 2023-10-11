package org.zim.gamsapi.User.interfaces;

import org.springframework.data.repository.CrudRepository;
import org.zim.gamsapi.User.User;

public interface IUserRepository extends CrudRepository<User, String> {

  //TODO this should work with optional
  User findByUsername(String username);

}
