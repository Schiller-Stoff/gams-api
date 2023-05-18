package org.zim.gamsapi.System.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

/**
 * Disables security for all requests
 */
@Configuration
public class DisableSecurityConfiguration {

  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return (web) -> web.ignoring().anyRequest();
  }

}
