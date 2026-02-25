package com.coworking_hub.app.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@EnableConfigurationProperties(UploadProperties.class)
public class StaticResourceConfig implements WebMvcConfigurer {

    private final UploadProperties uploadProperties;

    public StaticResourceConfig(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String publicPrefix = normalizePublicPrefix(uploadProperties.getPublicPrefix());
        Path uploadPath = Paths.get(uploadProperties.getBaseDir()).toAbsolutePath().normalize();
        String location = uploadPath.toUri().toString();

        registry.addResourceHandler(publicPrefix + "/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }

    private String normalizePublicPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "/uploads";
        }
        return prefix.startsWith("/") ? prefix : "/" + prefix;
    }
}
