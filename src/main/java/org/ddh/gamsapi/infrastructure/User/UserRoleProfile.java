package org.ddh.gamsapi.infrastructure.User;

import org.ddh.gamsapi.infrastructure.System.security.GAMSAPIAuthorities;
import org.springframework.security.core.GrantedAuthority;

import java.util.*;

/**
 * Parses flat Spring Security authorities into a structured user role profile.
 * Separates global/top-level roles from project-scoped roles.
 * <p>
 * Role conventions:
 * <ul>
 *   <li>Top roles: {@code super_admin}, {@code projects_admin}</li>
 *   <li>Project roles: {@code {projectAbbr}_admin}, {@code {projectAbbr}_editor},
 *       {@code {projectAbbr}_viewer}, {@code {projectAbbr}_viewer_{restriction}}</li>
 * </ul>
 */
public class UserRoleProfile {

  private static final String ROLE_PREFIX = GAMSAPIAuthorities.ROLE_PREFIX.name;

  /**
   * Known top-level roles. Any authority matching these exactly (after ROLE_ stripping)
   * is classified as a top role. Everything else with a recognized suffix is project-scoped.
   */
  private static final Set<String> TOP_ROLES = Set.of(
      GAMSAPIAuthorities.SUPER_ADMINISTRATOR.name,
      GAMSAPIAuthorities.PROJECTS_ADMINISTRATOR.name
  );

  /**
   * Recognized project role suffixes. Used to identify project-scoped authorities.
   * Order matters: longer suffixes checked first to avoid partial matches
   * (e.g., "viewer_X" must not be split at "_viewer").
   */
  private static final List<String> PROJECT_ROLE_SUFFIXES = List.of(
      "_admin",
      "_editor",
      "_viewer"  // also matches _viewer_SOME_RESTRICTION
  );

  private static final Set<String> IGNORED_PREFIXES = Set.of("SCOPE_", "OIDC_");

  private final List<String> topRoles;
  private final Map<String, List<String>> projectRoles;

  public UserRoleProfile(Collection<? extends GrantedAuthority> authorities) {
    List<String> tops = new ArrayList<>();
    Map<String, List<String>> projects = new TreeMap<>();

    for (GrantedAuthority ga : authorities) {
      String authority = ga.getAuthority();

      // Strip ROLE_ prefix
      String role = authority.startsWith(ROLE_PREFIX)
          ? authority.substring(ROLE_PREFIX.length())
          : authority;

      // Skip Spring-internal authorities
      if (isIgnored(role)) continue;

      // Check if it's a known top-level role
      if (TOP_ROLES.contains(role)) {
        tops.add(role);
        continue;
      }

      // Try to parse as project-scoped role
      parseProjectRole(role).ifPresent(parsed ->
          projects.computeIfAbsent(parsed.projectAbbr(), k -> new ArrayList<>())
              .add(parsed.roleType())
      );
    }

    this.topRoles = Collections.unmodifiableList(tops);
    this.projectRoles = Collections.unmodifiableMap(projects);
  }

  private boolean isIgnored(String role) {
    for (String prefix : IGNORED_PREFIXES) {
      if (role.startsWith(prefix)) return true;
    }
    return false;
  }

  /**
   * Attempts to parse a role string as a project-scoped role.
   * Looks for the FIRST occurrence of a known suffix (_admin, _editor, _viewer).
   * Everything before the suffix is the project abbreviation.
   * Everything from the suffix onward is the role type.
   *
   * Examples:
   *   "cantus_admin"              → projectAbbr="cantus", roleType="admin"
   *   "memo_editor"               → projectAbbr="memo",   roleType="editor"
   *   "roth_viewer_OVER_AGE_18"   → projectAbbr="roth",   roleType="viewer_OVER_AGE_18"
   *   "my_org_admin"              → projectAbbr="my_org",  roleType="admin"
   */
  private Optional<ParsedProjectRole> parseProjectRole(String role) {
    for (String suffix : PROJECT_ROLE_SUFFIXES) {
      int idx = role.indexOf(suffix);
      if (idx > 0) {
        String projectAbbr = role.substring(0, idx);
        String roleType = role.substring(idx + 1); // skip the leading '_'
        return Optional.of(new ParsedProjectRole(projectAbbr, roleType));
      }
    }
    return Optional.empty();
  }

  private record ParsedProjectRole(String projectAbbr, String roleType) {}

  public List<String> getTopRoles() { return topRoles; }
  public Map<String, List<String>> getProjectRoles() { return projectRoles; }
  public boolean hasTopRoles() { return !topRoles.isEmpty(); }
  public boolean hasProjectRoles() { return !projectRoles.isEmpty(); }
}
