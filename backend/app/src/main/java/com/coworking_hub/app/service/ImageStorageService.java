package com.coworking_hub.app.service;

import com.coworking_hub.app.config.UploadProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

    private final UploadProperties uploadProperties;

    public ImageStorageService(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    public String storeProfileImage(MultipartFile file) {
        return storeImage(file, "profiles");
    }

    public String storeSpaceImage(MultipartFile file, Long spaceId) {
        return storeImage(file, "spaces/" + spaceId);
    }

    public String storeImage(MultipartFile file, String relativeFolder) {
        validateImage(file);

        String extension = resolveExtension(file.getOriginalFilename(), file.getContentType());
        String filename = UUID.randomUUID() + extension;

        Path basePath = Paths.get(uploadProperties.getBaseDir()).toAbsolutePath().normalize();
        Path targetFolder = basePath.resolve(relativeFolder).normalize();
        Path targetFile = targetFolder.resolve(filename).normalize();

        ensureInsideBasePath(basePath, targetFile);

        try {
            Files.createDirectories(targetFolder);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Neuspesno cuvanje slike.", e);
        }

        String publicPrefix = normalizePublicPrefix(uploadProperties.getPublicPrefix());
        String normalizedFolder = relativeFolder.replace('\\', '/');
        return publicPrefix + "/" + normalizedFolder + "/" + filename;
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Slika je obavezna.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Dozvoljeni formati su JPG i PNG.");
        }
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (originalFilename != null) {
            String lower = originalFilename.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                return ".jpg";
            }
            if (lower.endsWith(".png")) {
                return ".png";
            }
        }

        if ("image/png".equalsIgnoreCase(contentType)) {
            return ".png";
        }

        return ".jpg";
    }

    private void ensureInsideBasePath(Path basePath, Path targetFile) {
        if (!targetFile.startsWith(basePath)) {
            throw new IllegalArgumentException("Neispravna putanja za cuvanje slike.");
        }
    }

    private String normalizePublicPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "/uploads";
        }
        return prefix.startsWith("/") ? prefix : "/" + prefix;
    }
}
