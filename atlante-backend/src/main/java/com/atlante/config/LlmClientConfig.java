package com.atlante.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class LlmClientConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmClientConfig.class);

    @Bean
    RestClient llmClient(@Value("${app.llm.base-url}") String baseUrl,
                         @Value("${app.llm.api-key:}") String apiKey) {

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("app.llm.api-key non impostata: le chiamate AI riceveranno 401");
        }
        log.info("client LLM verso {}", baseUrl);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(60));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }
}
