package com.coworking_hub.app.security;

public record AuthenticatedUser(
        Long userId,
        String username,
        String role,
        String status
) {
}
