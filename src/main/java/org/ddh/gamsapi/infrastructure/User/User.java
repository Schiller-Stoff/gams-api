package org.ddh.gamsapi.infrastructure.User;

import lombok.*;

import java.util.Set;

/**
 * Model representing an user in terms of user management.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class User {

  /**
   * The user id.
   */
  private String userid;

  /**
   * The user name.
   */
  private String username;

  /**
   * List of user authorities.
   */
  private Set<String> authorities;

}