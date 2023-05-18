package org.zim.gamsapi.User;

import lombok.Data;

/**
 * Model representing an user in terms of user management.
 */
@Data
public class User {
  public final String id;
  public final String info;
  public final String[] groups;
  public final String name;
}