package org.zim.gamsapi.User;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 *
 */
public class UserPrincipal implements org.springframework.security.core.userdetails.UserDetails {

  private final User user;

  public UserPrincipal(User user) {
    this.user = user;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return null;
  }

  @Override
  public String getPassword() {
    return user.getPassword();
  }

  @Override
  public String getUsername() {
    return user.getUsername();
  }

  @Override
  public boolean isAccountNonExpired() {
    // TODO implement
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    // TODO implement
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    //TODO implement
    return true;
  }

  @Override
  public boolean isEnabled() {
    // TODO implement
    return true;
  }
}
