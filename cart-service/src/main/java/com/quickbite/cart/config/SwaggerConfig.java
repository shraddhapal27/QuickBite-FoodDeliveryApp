package com.quickbite.cart.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI cartServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cart Service API")
                        .description("Shopping cart management API for the QuickBite Platform. "
                                + "Handles cart operations including add/remove items, "
                                + "quantity updates, promo codes, and cart clearing.")
                        .version("1.0")
                        .contact(new Contact()
                                .name("QuickBite Team")
                                .email("support@quickbite.com")));
    }
}
