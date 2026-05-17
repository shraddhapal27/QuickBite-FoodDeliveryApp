package com.quickbite.admin;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@SpringBootApplication
@EnableAdminServer
@EnableDiscoveryClient
public class AdminServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminServerApplication.class, args);
    }

    @Configuration(proxyBeanMethods = false)
    public static class SecuritySecureConfig {
        private final AdminServerProperties adminServer;

        public SecuritySecureConfig(AdminServerProperties adminServer) {
            this.adminServer = adminServer;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
            successHandler.setTargetUrlParameter("redirectTo");
            successHandler.setDefaultTargetUrl(this.adminServer.getContextPath() + "/");

            http.authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(new AntPathRequestMatcher(this.adminServer.getContextPath() + "/assets/**")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher(this.adminServer.getContextPath() + "/login")).permitAll()
                    .anyRequest().authenticated()
            ).formLogin(formLogin -> formLogin
                    .loginPage(this.adminServer.getContextPath() + "/login")
                    .successHandler(successHandler)
            ).logout(logout -> logout
                    .logoutUrl(this.adminServer.getContextPath() + "/logout")
            ).httpBasic(httpBasic -> {})
             .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringRequestMatchers(
                            new AntPathRequestMatcher(this.adminServer.getContextPath() + "/instances", "POST"),
                            new AntPathRequestMatcher(this.adminServer.getContextPath() + "/instances/*", "DELETE"),
                            new AntPathRequestMatcher(this.adminServer.getContextPath() + "/actuator/**")
                    )
            );
            return http.build();
        }
    }
}
