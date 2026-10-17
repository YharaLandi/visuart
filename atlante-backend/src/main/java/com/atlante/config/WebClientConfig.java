package com.atlante.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient tmdbWebClient(@Value("${app.tmdb.base-url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    public WebClient metWebClient(@Value("${app.met.base-url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}
