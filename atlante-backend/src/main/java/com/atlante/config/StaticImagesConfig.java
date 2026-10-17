package com.atlante.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class StaticImagesConfig implements WebMvcConfigurer {

    private final String storageDir;

    public StaticImagesConfig(@Value("${app.images.storage-dir}") String storageDir) {
        this.storageDir = storageDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + Path.of(storageDir).toAbsolutePath() + "/");
    }
}
