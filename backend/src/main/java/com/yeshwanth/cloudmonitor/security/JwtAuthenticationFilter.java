package com.yeshwanth.cloudmonitor.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Reads {@code Authorization: Bearer <jwt>} and, when the token verifies, puts an authenticated
 * principal with a {@code ROLE_<role>} authority into the security context.
 *
 * <p>An absent or invalid token is not rejected here: the filter simply leaves the request
 * anonymous and {@link SecurityConfig} decides whether that is allowed.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        bearerToken(request).ifPresent(token -> authenticate(request, token));
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        // Already authenticated: nothing to do.
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }
        try {
            JwtService.AuthToken authToken = jwtService.verify(token);
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + authToken.role()));
            var authentication = new UsernamePasswordAuthenticationToken(authToken.username(), null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException e) {
            logger.debug("Rejected bearer token: " + e.getMessage());
        }
    }

    private Optional<String> bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String token = header.substring(BEARER_PREFIX.length()).strip();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
