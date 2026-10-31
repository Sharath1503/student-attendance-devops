package com.attendance.security;

import com.attendance.config.AppProperties;
import com.attendance.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Creates and validates signed (HS256) JWT access tokens. */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    @Autowired
    public JwtService(AppProperties props) {
        this(props.jwt().secret(), props.jwt().expirationMinutes());
    }

    public JwtService(String secret, long expirationMinutes) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be set and at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(String username, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
                .signWith(key)
                .compact();
    }

    /**
     * Validates signature and expiry and returns the username.
     *
     * @throws JwtException when the token is malformed, tampered with or expired
     */
    public String validateAndGetUsername(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return claims.getSubject();
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }
}
