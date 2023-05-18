package org.zim.gamsapi.User.interfaces;

import jakarta.servlet.http.HttpServletRequest;
import org.zim.gamsapi.User.User;

import java.util.List;

/**
 * Reads out available OUATH2 token based information
 * and provides GAMS specific domain classes / implementations
 * for further processing.
 */
public interface IUserService {

  /**
   * Reads out available OUATH2 token based information and parses it
   * to User model.
   * @return {User} user model class
   */
  public User getCurrentUser(HttpServletRequest request);

  /**
   * Reads out groups stated in current active OUATH2 token
   * and returns it as string array.
   * @return {String[]} assigned groups to oauth2 user.
   */
  public List<String> getCurrentUserGroups(HttpServletRequest request);

}
