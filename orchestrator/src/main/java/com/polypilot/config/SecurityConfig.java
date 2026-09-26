package com.polypilot.config;

import com.polypilot.auth.JwtFilter;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@AllArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — we use JWT in headers, not cookies
                .csrf(AbstractHttpConfigurer::disable)

                // No server-side sessions — every request must carry its own JWT
                .sessionManagement(s -> s
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Route-level authorization rules — evaluated top to bottom, first match wins.
                // /api/auth/** is deliberately NOT a blanket permitAll: GET /api/auth/me must
                // require a valid JWT like everything else, so only the actual public auth
                // routes (login, SIWE nonce/verify) are listed explicitly.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login",
                                "/api/auth/siwe/nonce",
                                "/api/auth/siwe/verify",
                                "/actuator/health",
                                "/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )

                // Insert our JWT filter before Spring's own UsernamePasswordAuthenticationFilter.
                // This means the SecurityContext is populated before authorization rules are checked.
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt is the standard for password hashing.
     * Strength 12 = ~250ms per hash on a modern machine — slow enough to resist brute force.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}