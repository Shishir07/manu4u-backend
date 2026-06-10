package com.manu4u.tools.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI manu4uOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ManU4U Tools API")
                        .description("API for Manchester United tool wrappers - provides access to fixtures, events, and lineups via API-Football")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("ManU4U")
                                .email("support@manu4u.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
