package com.yeshwanth.cloudmonitor.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final String secret = System.getenv().getOrDefault("JWT_SECRET", "change-this-development-secret-to-a-long-random-value-1234567890");
    private SecretKey key() { return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); }
    public String generate(String username, String role) {
        return Jwts.builder().subject(username).claim("role", role).issuedAt(new Date()).expiration(new Date(System.currentTimeMillis()+86400000)).signWith(key()).compact();
    }
    public String username(String token) { return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload().getSubject(); }
}
