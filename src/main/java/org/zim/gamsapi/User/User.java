package org.zim.gamsapi.User;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.zim.gamsapi.Project.Project;

import java.util.List;
import java.util.Set;

/**
 * Model representing an user in terms of user management.
 */
@Data
// 'user' is a reserverd keyowrd in PSQL (therefore users table)
@Table(name = "users")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {

  @Id
  @NotNull
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long userid;

  @Column(nullable = false, unique = true)
  private String username;

  @ToString.Exclude
  private String password;

  @ManyToMany
  @JoinTable(
          name = "projects_users",
          joinColumns = @JoinColumn(name = "users_userid"),
          inverseJoinColumns = @JoinColumn(name = "project_id")
  )
  private List<Project> projects;

  @ElementCollection
  @NotNull
  private Set<String> roles;
}