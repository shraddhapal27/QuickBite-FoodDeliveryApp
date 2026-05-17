package com.quickbite.review.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI reviewServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Review Service API")
                        .description("Review and Rating management API for the QuickBite Platform. "
                                + "Handles food and delivery reviews, average rating computation, "
                                + "and admin moderation.")
                        .version("1.0")
                        .contact(new Contact()
                                .name("QuickBite Team")
                                .email("support@quickbite.com")));
    }
}
