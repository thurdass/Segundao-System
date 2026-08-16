package com.thurdass.system2a.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey secretKey;
    private final long expirationMillis;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-hours:24}") long expirationHours
    ) {
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must have at least 32 characters");
        }
        secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        expirationMillis = expirationHours * 3_600_000L;
    }

    public String generate(UserDetails authenticatedUser) {
        Instant currentInstant = Instant.now();
        return Jwts.builder()
                .subject(authenticatedUser.getUsername())
                .issuedAt(Date.from(currentInstant))
                .expiration(new Date(currentInstant.toEpochMilli() + expirationMillis))
                .signWith(secretKey)
                .compact();
    }

    public String username(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getSubject();
    }

    public boolean valid(String token, UserDetails authenticatedUser) {
        try {
            return authenticatedUser.getUsername().equalsIgnoreCase(username(token))
                    && Jwts.parser().verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration()
                    .after(new Date());
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }
}
