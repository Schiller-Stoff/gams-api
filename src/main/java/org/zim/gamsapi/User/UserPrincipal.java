package org.zim.gamsapi.User;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 *
 */
@Slf4j
public class UserPrincipal implements org.springframework.security.core.userdetails.UserDetails {

  private final User user;

  public UserPrincipal(User user) {
    this.user = user;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    // grant authorities for assigned projects ("cantus" or "derla")
    List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>(
            user.getProjects().stream()
            .map(project -> new SimpleGrantedAuthority(project.getProjectAbbr()))
            .toList()
    );
    // grant authorities per assigned role (e.g. "administrator" or "editor")
    user.getRoles().forEach(id -> authorities.add(new SimpleGrantedAuthority(id)));
    if(authorities.size() == 0){
      String msg = String.format("No authorities (assigned projects and roles) found for user %s", user.getUsername());
      log.warn(msg);
    }
    return authorities;
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
