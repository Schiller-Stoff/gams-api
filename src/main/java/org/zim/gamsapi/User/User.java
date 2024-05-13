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
// 'user' is a reserverd keyowrd in PSQL (therefore users table)
@Table(name = "users")
@Entity
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

  /**
   * GAMS projects assigned to the user
   */
  // fetch-type is necessary for authorization https://stackoverflow.com/questions/11746499/how-to-solve-the-failed-to-lazily-initialize-a-collection-of-role-hibernate-ex
  @ManyToMany(fetch = FetchType.EAGER)
  @ToString.Exclude
  private Set<Project> projects;

}