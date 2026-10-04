package com.yeshwanth.cloudmonitor.security;

import com.yeshwanth.cloudmonitor.config.JwtProperties;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-long-enough-for-hs256-signing";

    private JwtService service(String secret, Duration ttl) {
        return new JwtService(new JwtProperties(secret, ttl));
    }

    @Test
    @DisplayName("an issued token round-trips username and role")
    void issuesAndVerifies() {
        JwtService jwtService = service(SECRET, Duration.ofHours(1));

        String token = jwtService.issueToken("admin", "ADMIN");
        JwtService.AuthToken authToken = jwtService.verify(token);

        assertThat(authToken.username()).isEqualTo("admin");
        assertThat(authToken.role()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("a token signed with another secret is rejected")
    void rejectsForeignSignature() {
        String token = service(SECRET, Duration.ofHours(1)).issueToken("admin", "ADMIN");
        JwtService other = service("a-completely-different-secret-value-32-chars!", Duration.ofHours(1));

        assertThatThrownBy(() -> other.verify(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("an expired token is rejected")
    void rejectsExpiredToken() {
        JwtService jwtService = service(SECRET, Duration.ofMillis(-1000));

        String token = jwtService.issueToken("admin", "ADMIN");

        assertThatThrownBy(() -> jwtService.verify(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("a short signing secret fails fast at startup")
    void rejectsWeakSecret() {
        assertThatThrownBy(() -> service("too-short", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.jwt.secret");
    }
}
