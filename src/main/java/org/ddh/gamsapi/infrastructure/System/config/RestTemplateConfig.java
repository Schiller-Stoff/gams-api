package org.ddh.gamsapi.infrastructure.System.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        var restTemplate = new RestTemplate();
        // RestTemplate's StringHttpMessageConverter defaults to ISO-8859-1,
        // which corrupts non-ASCII characters in SPARQL Update payloads.
        restTemplate.getMessageConverters().stream()
          .filter(StringHttpMessageConverter.class::isInstance)
          .map(StringHttpMessageConverter.class::cast)
          .forEach(converter -> converter.setDefaultCharset(StandardCharsets.UTF_8));
        return new RestTemplate();
    }
}
