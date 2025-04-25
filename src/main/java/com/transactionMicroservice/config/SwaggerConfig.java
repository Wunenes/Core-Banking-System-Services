package com.transactionMicroservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI accountServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Account Service API")
                        .description("Banking account management (No Auth)")
                        .version("1.0")
                );
    }
}