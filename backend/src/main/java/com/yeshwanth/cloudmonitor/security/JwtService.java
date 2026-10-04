package com.yeshwanth.cloudmonitor.security;

import com.yeshwanth.cloudmonitor.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and verifies the HMAC-signed access tokens used by the dashboard.
 */
@Service
public class JwtService {

    /** Minimum key length required by HS256 (256 bits). */
    private static final int MIN_SECRET_LENGTH = 32;
    private static final String ROLE_CLAIM = "role";

    private final SecretKey signingKey;
    private final Duration accessTokenTtl;

    public JwtService(JwtProperties properties) {
        String secret = properties.secret();
        if (secret == null || secret.strip().length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least " + MIN_SECRET_LENGTH
                            + " characters. Set the JWT_SECRET environment variable to a long random value.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.strip().getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = properties.accessTokenTtl();
    }

    public String issueToken(String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim(ROLE_CLAIM, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * @throws JwtException if the signature is invalid or the token is expired or malformed
     */
    public AuthToken verify(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AuthToken(claims.getSubject(), claims.get(ROLE_CLAIM, String.class));
    }

    public Duration accessTokenTtl() {
        return accessTokenTtl;
    }

    /** The identity carried by a validated token. */
    public record AuthToken(String username, String role) {
    }
}
