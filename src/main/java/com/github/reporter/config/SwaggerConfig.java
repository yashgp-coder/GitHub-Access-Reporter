package com.github.reporter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GitHub Access Reporter API")
                        .version("1.0.0")
                        .description("Generates access reports showing which users have access " +
                                     "to which repositories within a GitHub organization.")
                        .contact(new Contact()
                                .name("GitHub Access Reporter")
                                .url("https://github.com"))
                        .license(new License()
                                .name("MIT License")));
    }
}