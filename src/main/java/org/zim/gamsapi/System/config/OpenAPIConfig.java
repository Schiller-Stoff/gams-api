package org.zim.gamsapi.System.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

  @Value("${server.port:18085}")
  private String serverPort;

  @Value("${spring.application.name:gams-api}")
  private String applicationName;

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(getInfo())
        .servers(getServers())
        .addSecurityItem(new SecurityRequirement().addList("oauth2"))
        .components(new io.swagger.v3.oas.models.Components()
            .addSecuritySchemes("oauth2", createOAuth2Scheme()))
        .tags(List.of(
            new Tag().name("Projects").description("Project management operations"),
            new Tag().name("Digital Objects").description("Digital object CRUD operations"),
            new Tag().name("Datastreams").description("Datastream content and metadata operations"),
            new Tag().name("Search").description("Search and filtering operations"),
            new Tag().name("Integration").description("Integration services (hidden from public API)")
        ));
  }

  private Info getInfo() {
    return new Info()
        .title("GAMS5 API")
        .version("1.0.0")
        .description("""
                    REST API for GAMS5 (Geisteswissenschaftliches Asset Management System)
                    
                    ## Overview
                    This API provides comprehensive access to digital objects, projects, and datastreams
                    with support for millions of digital objects and tens of millions of datastreams.
                    
                    ## Authentication
                    This API uses OAuth2 authentication via Keycloak.
                    
                    ## Rate Limiting
                    API requests are rate-limited to ensure optimal performance at scale.
                    """)
        .license(new License()
            .name("Apache 2.0")
            .url("https://www.apache.org/licenses/LICENSE-2.0.html"))
        .contact(new Contact()
            .name("Sebastian Schiller-Stoff / ZIM")
            .email("sebastian.schiller-stoff@uni-graz.at")
            .url("https://zimlab.uni-graz.at/gams5"));
  }

  private List<Server> getServers() {
    // TODO hardcoded urls should be replaced with properties or environment variables
    return List.of(
        new Server()
            .url("http://localhost:" + serverPort)
            .description("Development server"),
        new Server()
            .url("https://gams.uni-graz.at")
            .description("Production server")
    );
  }

  private SecurityScheme createOAuth2Scheme() {
    return new SecurityScheme()
        .type(SecurityScheme.Type.OAUTH2)
        .description("OAuth2 authentication via Keycloak")
        .flows(new io.swagger.v3.oas.models.security.OAuthFlows()
            .authorizationCode(new io.swagger.v3.oas.models.security.OAuthFlow()
                // TODO keycloak URLs should be configured via properties
                .authorizationUrl("https://your-keycloak-server/auth")
                // TODO token url should be configured via properties
                .tokenUrl("https://your-keycloak-server/token")
                .scopes(new io.swagger.v3.oas.models.security.Scopes()
                    .addString("read", "Read access")
                    .addString("write", "Write access"))));
  }
}