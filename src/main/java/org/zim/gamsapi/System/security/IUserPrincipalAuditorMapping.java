package org.zim.gamsapi.System.security;

import org.springframework.data.domain.AuditorAware;

/**
 * Interface for the UserPrincipalAuditorMapping (interface needed to override bean in testing context)
 *
 * Defines how a user principal is being used to create auditing marks in the persistence layer,
 * like CreatedBy or ModifiedBy.
 *
 *
 * https://mayankposts.medium.com/database-auditing-in-spring-boot-with-spring-security-context-and-spring-data-jpa-9215b43744bb
 * https://www.baeldung.com/database-auditing-jpa
 */
public interface IUserPrincipalAuditorMapping extends AuditorAware<String> {
}
