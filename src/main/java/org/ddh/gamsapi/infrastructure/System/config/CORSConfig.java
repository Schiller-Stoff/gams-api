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

    // allow public for integration api
    registry.addMapping("/api/integration/v1/**")
        .allowedOriginPatterns(gamsCors.getAllowedOriginPatterns())
        .allowedMethods("GET", "OPTIONS", "HEAD")
        .allowedHeaders("*")
        .allowCredentials(false) // Public endpoints don't need credentials
        .maxAge(gamsCors.getMaxAge());

    // allow public for curation api
    registry.addMapping("/api/curation/v1/**")
        .allowedOriginPatterns(gamsCors.getAllowedOriginPatterns())
        .allowedMethods("GET", "HEAD", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false)
        .maxAge(gamsCors.getMaxAge());

    // OpenAPI/Swagger endpoints
    registry.addMapping("/api/openapi/**")
        .allowedOriginPatterns(gamsCors.getAllowedOriginPatterns())
        .allowedMethods("GET", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false)
        .maxAge(gamsCors.getMaxAge());

    // Authenticated endpoints requiring credentials (GET, OPTIONS, HEAD are handled before and public, inside here to secure 'forgotten' paths)
    registry.addMapping("/api/**")
        .allowedOriginPatterns(gamsCors.getAllowedOriginPatterns())
        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD")
        .allowedHeaders("*")
        .allowCredentials(true) // Required for OAuth2/Keycloak authentication
        .maxAge(gamsCors.getMaxAge());


  }
}