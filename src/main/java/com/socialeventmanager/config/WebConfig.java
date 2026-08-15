package com.socialeventmanager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class WebConfig {

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration prometheusConfig = new CorsConfiguration();
        prometheusConfig.setAllowedOriginPatterns(List.of("*"));
        prometheusConfig.setAllowedHeaders(List.of("*"));
        prometheusConfig.setAllowedMethods(List.of("GET", "OPTIONS"));

        CorsConfiguration apiConfig = new CorsConfiguration();
        apiConfig.setAllowCredentials(true);
        apiConfig.setAllowedOrigins(allowedOrigins);
        apiConfig.setAllowedHeaders(List.of("*"));
        apiConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/actuator/prometheus", prometheusConfig);
        source.registerCorsConfiguration("/**", apiConfig);

        return source;
    }
}