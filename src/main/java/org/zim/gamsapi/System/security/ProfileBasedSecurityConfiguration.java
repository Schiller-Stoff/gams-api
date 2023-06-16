package org.zim.gamsapi.System.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import java.util.Arrays;
import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Conditionally enables spring security for different profiles.
 */
@Configuration
@Slf4j
public class ProfileBasedSecurityConfiguration {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, Environment environment) throws Exception {

    final String PROD_PROFILE = "prod";

    String[] currentlyActiveProfiles = environment.getActiveProfiles();
    boolean prodProfileActive = Arrays.asList(currentlyActiveProfiles).contains(PROD_PROFILE);

    if(prodProfileActive){
      log.info("*** Activating oauth2 based security because detected prod profile");
      http
        .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()
        )
        .oauth2Login(withDefaults());
    } else {
      log.info("*** Deactivating spring security for currently active profile (not prod profile)");
      http.authorizeHttpRequests(authorize -> {
        authorize.anyRequest().permitAll();
      });
    }
    return http.build();
  }

}
