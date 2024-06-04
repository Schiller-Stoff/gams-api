package org.zim.gamsapi.User;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.zim.gamsapi.Project.Project;
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
  @Id
  @NotNull
  private String userid;

  /**
   * The user name.
   */
  @Column(nullable = false, unique = true)
  @NotBlank
  private String username;

}