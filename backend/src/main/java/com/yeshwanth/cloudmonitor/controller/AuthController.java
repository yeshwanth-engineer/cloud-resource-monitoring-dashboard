package com.yeshwanth.cloudmonitor.controller;

import com.yeshwanth.cloudmonitor.config.DemoAuthProperties;
import com.yeshwanth.cloudmonitor.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * Issues a bearer token for the configured single development account.
 *
 * <p>Swap this controller's lookup for a {@code UserDetailsService} plus a password-encoded
 * database table before production; the rest of the security chain does not need to change.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;
    private final DemoAuthProperties demoAccount;

    public AuthController(JwtService jwtService, DemoAuthProperties demoAccount) {
        this.jwtService = jwtService;
        this.demoAccount = demoAccount;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        // Constant-time-ish compare is unnecessary here: this is a single demo account. Use
        // PasswordEncoder.matches(...) once real, hash-stored credentials exist.
        boolean matches = demoAccount.username().equals(request.username())
                && demoAccount.password().equals(request.password());
        if (!matches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        String token = jwtService.issueToken(request.username(), demoAccount.role());
        return new AuthResponse(token, "Bearer", demoAccount.role(), Instant.now().plus(jwtService.accessTokenTtl()));
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record AuthResponse(String token, String tokenType, String role, Instant expiresAt) {
    }
}
