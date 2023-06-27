package org.zim.gamsapi.System.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Conditionally enables spring security for different profiles.
 */
@Configuration
@Slf4j
public class DeactivateSpringSecurityConfiguration {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    log.info("*** Deactivating spring security ***");
    http.authorizeHttpRequests(authorize -> {
      authorize.anyRequest().permitAll();
    });

    return http.build();

  }
}
