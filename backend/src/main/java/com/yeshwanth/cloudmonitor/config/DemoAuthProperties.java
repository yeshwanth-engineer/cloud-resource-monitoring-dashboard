package com.yeshwanth.cloudmonitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Single in-memory development account, bound from the {@code app.demo-auth.*} keys.
 *
 * <p>This exists so the dashboard can be tried out without provisioning a user store. Replace it
 * with a {@code UserDetailsServiceImpl} backed by a database before any real deployment.
 */
@ConfigurationProperties(prefix = "app.demo-auth")
public record DemoAuthProperties(String username, String password, String role) {

    public DemoAuthProperties {
        username = (username == null || username.isBlank()) ? "admin" : username;
        password = (password == null || password.isBlank()) ? "admin123" : password;
        role = (role == null || role.isBlank()) ? "ADMIN" : role.toUpperCase();
    }
}
