package org.zim.gamsapi.User;

import lombok.*;

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

}