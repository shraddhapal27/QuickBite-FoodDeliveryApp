package com.quickbite.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notification Service API")
                        .description("Notification management API for the QuickBite Platform. "
                                + "Handles in-app notifications, email/SMS dispatch, "
                                + "read-state management, and bulk notifications.")
                        .version("1.0")
                        .contact(new Contact()
                                .name("QuickBite Team")
                                .email("support@quickbite.com")));
    }
}
