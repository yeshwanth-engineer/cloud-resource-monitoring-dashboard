package com.yeshwanth.cloudmonitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Browser origins allowed to call the API directly. Bound from {@code app.cors.*}.
 *
 * <p>Not needed when the frontend is served behind the same origin (the Vite dev proxy and the
 * shipped nginx config both forward {@code /api} to this service).
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = (allowedOrigins == null || allowedOrigins.isEmpty())
                ? List.of("http://localhost:5173", "http://localhost:3000")
                : List.copyOf(allowedOrigins);
    }
}
