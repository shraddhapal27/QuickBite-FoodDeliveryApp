package com.quickbite.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("QuickBite API Gateway")
                        .description("Centralized API Gateway for the QuickBite Food Delivery Platform. "
                                + "Use the dropdown above to switch between microservice APIs.")
                        .version("1.0")
                        .contact(new Contact()
                                .name("QuickBite Team")
                                .email("support@quickbite.com")));
    }
}
