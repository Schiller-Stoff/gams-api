package org.ddh.gamsapi.infrastructure.System.security.exceptions;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.AuthenticationException;

/**
 * If user authentication failed.
 */
public class UserAuthenticationRequiredException extends AuthenticationException {

  public UserAuthenticationRequiredException(@Nullable String msg, Throwable cause) {
    super(msg, cause);
  }

  public UserAuthenticationRequiredException(@Nullable String msg) {
    super(msg);
  }
}
