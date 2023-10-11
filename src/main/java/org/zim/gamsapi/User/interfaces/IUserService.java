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

  User findByUsername(String username);

}
