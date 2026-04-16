package org.ddh.gamsapi.infrastructure.System.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.ddh.gamsapi.infrastructure.System.configproperties.GAMSCorsProperties;

import java.util.Arrays;

/**
 * CORS configuration for GAMS API.
 * Handles cross-origin requests for browser-based clients.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class CORSConfig implements WebMvcConfigurer {


  private final GAMSCorsProperties gamsCors;


  @Override
  public void addCorsMappings(CorsRegistry registry) {

    log.info("*** Setting up CORS with variables");
    log.info("* Allowed origin patterns: {}", Arrays.stream(gamsCors.getAllowedOriginPatterns()).toList());
    log.info("*** Finished setup of GAMS variables");

    // Integration: public reads + public writes (for POST queries), uncredentialed
    registry.addMapping("/api/integration/**")
        .allowedOriginPatterns(gamsCors.getAllowedOriginPatterns())
        .allowedMethods("GET", "POST", "HEAD", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(gamsCors.getMaxAge());

    // Curation: mixed trust, browser uses cookies → credentialed full CRUD
    registry.addMapping("/api/curation/**")
        .allowedOriginPatterns(gamsCors.getAllowedOriginPatterns())
        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(gamsCors.getMaxAge());

    // OpenAPI docs: public reads only
    registry.addMapping("/api/openapi/**")
        .allowedOriginPatterns(gamsCors.getAllowedOriginPatterns())
        .allowedMethods("GET", "HEAD", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false)
        .maxAge(gamsCors.getMaxAge());

    // Auth infrastructure: credentialed (login/logout/callback)
    registry.addMapping("/api/auth/**")
        .allowedOriginPatterns(gamsCors.getAllowedOriginPatterns())
        .allowedMethods("GET", "POST", "HEAD", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(gamsCors.getMaxAge());

    // user info
    registry.addMapping("/api/**")
        .allowedOriginPatterns(gamsCors.getAllowedOriginPatterns())
        .allowedMethods("GET", "POST", "HEAD", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(gamsCors.getMaxAge());


  }
}