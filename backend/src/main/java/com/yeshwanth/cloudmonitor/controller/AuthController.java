package com.yeshwanth.cloudmonitor.controller;

import com.yeshwanth.cloudmonitor.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JwtService jwt;
    public AuthController(JwtService jwt) { this.jwt = jwt; }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> login(@RequestBody LoginRequest request) {
        // Demo account for local development. Replace with PostgreSQL user lookup in production.
        if ("admin".equals(request.username()) && "admin123".equals(request.password()))
            return Map.of("token", jwt.generate(request.username(), "ADMIN"), "role", "ADMIN");
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
    public record LoginRequest(String username, String password) {}
}
