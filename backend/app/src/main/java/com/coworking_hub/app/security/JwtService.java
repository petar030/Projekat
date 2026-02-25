package com.coworking_hub.app.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final String jwtSecret;
    private final long accessTokenExpirationSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.access-token-expiration-seconds:3600}") long accessTokenExpirationSeconds
    ) {
        this.jwtSecret = jwtSecret;
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
    }

    @PostConstruct
    void validateConfiguration() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret mora biti Base64 kljuc od najmanje 256 bita (32 bajta)");
        }
    }

    public String generateAccessToken(Long userId, String username, String role, String status) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(accessTokenExpirationSeconds);

        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .claim("role", role)
                .claim("status", status)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return extractAllClaims(token).get("uid", Long.class);
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public String extractStatus(String token) {
        return extractAllClaims(token).get("status", String.class);
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public long getRemainingSeconds(String token) {
        long remainingMillis = extractExpiration(token).getTime() - System.currentTimeMillis();
        return Math.max(0, remainingMillis / 1000);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isTokenValidForUsername(String token, String expectedUsername) {
        try {
            String username = extractUsername(token);
            return username.equals(expectedUsername) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public Optional<String> resolveTokenFromAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return Optional.empty();
        }

        String prefix = "Bearer ";
        if (!authorizationHeader.startsWith(prefix)) {
            return Optional.empty();
        }

        String token = authorizationHeader.substring(prefix.length()).trim();
        if (token.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(token);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
