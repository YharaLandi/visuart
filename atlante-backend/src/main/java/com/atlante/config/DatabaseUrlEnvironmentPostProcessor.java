package com.atlante.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Render (e Heroku) forniscono il database come DATABASE_URL=postgres://user:pass@host:port/db.
 * Spring vuole un URL JDBC con credenziali separate: qui lo si converte prima dell'avvio.
 * Se DB_URL è impostata esplicitamente, ha la precedenza.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication application) {
        String raw = env.getProperty("DATABASE_URL");
        if (raw == null || raw.isBlank() || env.containsProperty("DB_URL")
                || !(raw.startsWith("postgres://") || raw.startsWith("postgresql://"))) {
            return;
        }
        URI uri = URI.create(raw);
        String[] credentials = uri.getUserInfo() != null ? uri.getUserInfo().split(":", 2) : new String[0];
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;

        Map<String, Object> props = new HashMap<>();
        props.put("DB_URL", "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath());
        if (credentials.length > 0) props.put("DB_USER", credentials[0]);
        if (credentials.length > 1) props.put("DB_PASSWORD", credentials[1]);
        env.getPropertySources().addFirst(new MapPropertySource("databaseUrl", props));
    }
}
