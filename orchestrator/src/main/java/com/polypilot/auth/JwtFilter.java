package com.polypilot.auth;

import com.polypilot.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Runs once per request, before Spring Security's own authentication filter.
 * <p>
 * Responsibilities:
 * 1. Extract the JWT from the Authorization header
 * 2. Validate it
 * 3. If valid, populate the SecurityContext with the user's identity and role
 * <p>
 * If there's no token or it's invalid, we do nothing — Spring Security will
 * then apply the rules in SecurityConfig and return a 401 if the route is protected.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // If no Bearer token is present, skip — don't block the request here.
        // SecurityConfig decides whether the route requires auth.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // strip "Bearer "

        if (!jwtService.isValid(token)) {
            // Invalid or expired token — clear context and continue.
            // Spring Security will return 401 for protected routes.
            SecurityContextHolder.clearContext();
            chain.doFilter(request, response);
            return;
        }

        UUID userId = jwtService.extractUserId(token);

        var authority = new SimpleGrantedAuthority("ROLE_" + jwtService.extractRole(token));

        var authentication = new UsernamePasswordAuthenticationToken(
                userId, null, List.of(authority)
        );

        // Store in the SecurityContext — Spring Security reads this to authorize the request
        SecurityContextHolder.getContext().setAuthentication(authentication);

        chain.doFilter(request, response);
    }
}