package com.coworking_hub.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;

@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    private String baseDir = "uploads";
    private String publicPrefix = "/uploads";

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public String getPublicPrefix() {
        return publicPrefix;
    }

    public void setPublicPrefix(String publicPrefix) {
        this.publicPrefix = publicPrefix;
    }

    public Path resolveBasePath() {
        String configuredBaseDir = (baseDir == null || baseDir.isBlank()) ? "uploads" : baseDir.trim();
        Path configuredPath = Paths.get(configuredBaseDir);
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }
        Path workingDir = Paths.get(System.getProperty("user.dir", "."))
                .toAbsolutePath()
                .normalize();
        return workingDir.resolve(configuredPath).normalize();
    }
}
