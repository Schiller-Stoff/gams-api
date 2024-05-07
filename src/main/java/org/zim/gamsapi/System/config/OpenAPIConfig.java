package org.zim.gamsapi.System.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(getInfo());
    }

    private Info getInfo() {
        return new Info()
                .title("GAMS5 API")
                .version("0.0.1")  // TODO how to update?
                .description("REST-API Documentation of GAMS5")
                .license(
                        new License()  // TODO finally what license?
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")
                )
                .contact(
                        new Contact()
                                .name("Sebastion Schiller-Stoff / ZIM")
                                .email("sebastian.schiller-stoff@uni-graz.at")
                                .url("https://zimlab.uni-graz.at/gams5")
                );
    }
}