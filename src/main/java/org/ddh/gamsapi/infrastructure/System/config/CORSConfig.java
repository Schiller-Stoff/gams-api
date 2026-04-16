package org.ddh.gamsapi.infrastructure.System.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.ddh.gamsapi.infrastructure.System.configproperties.GAMSCorsProperties;

/**
 * CORS configuration for GAMS API.
 * Handles cross-origin requests for browser-based clients.
 */
@Configuration
@RequiredArgsConstructor
public class CORSConfig implements WebMvcConfigurer {


  private final GAMSCorsProperties gamsCors;


  @Override
  public void addCorsMappings(CorsRegistry registry) {
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

    // Authenticated endpoints requiring credentials
    registry.addMapping("/api/v1/**")
        .allowedOrigins(gamsCors.getAllowedOrigins())
        .allowedOriginPatterns(gamsCors.getAllowedOriginPatterns())
        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true) // Required for OAuth2/Keycloak authentication
        .maxAge(gamsCors.getMaxAge());

    // OpenAPI/Swagger endpoints
    registry.addMapping("/api/v1/openapi/**")
        .allowedOrigins(gamsCors.getAllowedOrigins())
        .allowedOriginPatterns(gamsCors.getAllowedOriginPatterns())
        .allowedMethods("GET", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false)
        .maxAge(gamsCors.getMaxAge());
  }
}