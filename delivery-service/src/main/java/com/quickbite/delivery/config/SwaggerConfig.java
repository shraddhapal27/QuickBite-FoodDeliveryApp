package com.quickbite.delivery.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI deliveryServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Delivery Service API")
                        .description("Delivery agent management API for the QuickBite Platform. "
                                + "Handles agent registration, verification, availability, location tracking, "
                                + "order assignment, and delivery lifecycle.")
                        .version("1.0")
                        .contact(new Contact()
                                .name("QuickBite Team")
                                .email("support@quickbite.com")));
    }
}
