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
    log.info("* Allowed origins: {}", Arrays.stream(gamsCors.getAllowedOrigins()).toList());
    log.info("* Allowed origin patterns: {}", Arrays.stream(gamsCors.getAllowedOriginPatterns()).toList());
    log.info("*** Finished setup of GAMS variables");

    // TODO allow only pattern OR origins! --> better would be just patterns!

    // Public API endpoints (integration services)
    registry.addMapping("/api/integration/v1/**")
        .allowedOrigins(gamsCors.getAllowedOrigins())
        .allowedOriginPatterns(gamsCors.getAllowedOriginPatterns())
        .allowedMethods("GET", "POST", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false) // Public endpoints don't need credentials
        .maxAge(gamsCors.getMaxAge());

    // Public read-only endpoints
    registry.addMapping("/api/curation/v1/projects/**")
        .allowedOrigins(gamsCors.getAllowedOrigins())
        .allowedOriginPatterns(gamsCors.getAllowedOriginPatterns())
        .allowedMethods("GET", "HEAD", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false)
        .maxAge(gamsCors.getMaxAge());

    // OpenAPI/Swagger endpoints
    registry.addMapping("/api/openapi/**")
        .allowedOrigins(gamsCors.getAllowedOrigins())
        .allowedOriginPatterns(gamsCors.getAllowedOriginPatterns())
        .allowedMethods("GET", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false)
        .maxAge(gamsCors.getMaxAge());

    // Authenticated endpoints requiring credentials
    registry.addMapping("/api/*/v1/**")
        .allowedOrigins(gamsCors.getAllowedOrigins())
        .allowedOriginPatterns(gamsCors.getAllowedOriginPatterns())
        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true) // Required for OAuth2/Keycloak authentication
        .maxAge(gamsCors.getMaxAge());


  }
}