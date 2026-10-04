package com.yeshwanth.cloudmonitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Token signing settings, bound from the {@code app.jwt.*} keys.
 *
 * @param secret          HMAC signing key; must be at least 32 characters.
 * @param accessTokenTtl  how long an issued token stays valid.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl) {

    private static final Duration DEFAULT_TTL = Duration.ofHours(8);

    public JwtProperties {
        accessTokenTtl = (accessTokenTtl == null) ? DEFAULT_TTL : accessTokenTtl;
    }
}
