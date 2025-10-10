package org.zim.gamsapi.infrastructure.System.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for content negotiation to support both JSON and XML responses.
 * Enables automatic format selection based on Accept headers or URL extensions.
 */
@Configuration
public class ContentNegotiationConfig implements WebMvcConfigurer {

  public static final String FORMAT_URL_PARAMETER = "format";


  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    configurer
        // Enable path extension negotiation (e.g., /api/v1/projects.xml)
        //.favorPathExtension(false) // Deprecated in Spring Boot 3.x, use Accept header
        .favorParameter(true)      // Enable format parameter (?format=xml)
        .parameterName(FORMAT_URL_PARAMETER)
        .ignoreAcceptHeader(false) // Don't ignore Accept header
        .useRegisteredExtensionsOnly(false)
        .defaultContentType(MediaType.APPLICATION_JSON)
        .mediaType("json", MediaType.APPLICATION_JSON)
        .mediaType("xml", MediaType.APPLICATION_XML);
  }
}
