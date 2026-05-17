package com.quickbite.restaurant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "com.quickbite")
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = "com.quickbite")
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = "com.quickbite")
@EnableDiscoveryClient
public class DeliveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliveryServiceApplication.class, args);
    }
}
