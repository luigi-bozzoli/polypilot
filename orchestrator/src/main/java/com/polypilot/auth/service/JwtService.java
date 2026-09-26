package com.polypilot.auth.service;


import com.polypilot.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtService {

    // Minimum decoded key length for HS256 (jjwt's Keys.hmacShaKeyFor requires >= 256 bits).
    private static final int MIN_KEY_BYTES = 32;

    // Rotated dev default (was application.yml's hardcoded fallback) — matched verbatim since
    // base64 is case-sensitive; rejected outright so a copy-pasted or forgotten .env value can't
    // silently sign real tokens.
    private static final String ROTATED_DEV_DEFAULT = "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv72ol/pe/Unols=";

    // Obvious placeholders, matched case-insensitively.
    private static final Set<String> WEAK_SECRET_WORDS = Set.of(
            "change-me", "changeme", "replace_me", "replaceme", "secret"
    );

    private final String secret;

    // Token validity: 24 hours in milliseconds
    private static final long EXPIRATION_MS = 86_400_000L;

    // Claims keys — constants avoid typos across generate/extract methods
    private static final String CLAIM_ROLE = "role";

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.secret = validateSecret(secret);
    }

    private static String validateSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "jwt.secret (JWT_SECRET) must be set — generate one with: openssl rand -base64 32");
        }
        String trimmed = secret.trim();
        if (trimmed.equals(ROTATED_DEV_DEFAULT) || WEAK_SECRET_WORDS.contains(trimmed.toLowerCase())) {
            throw new IllegalStateException(
                    "jwt.secret (JWT_SECRET) must not be a known default or placeholder value — "
                            + "generate one with: openssl rand -base64 32");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException e) {
            // jjwt's Decoders.BASE64 throws io.jsonwebtoken.io.DecodingException, not IllegalArgumentException.
            throw new IllegalStateException("jwt.secret (JWT_SECRET) must be valid base64", e);
        }
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret (JWT_SECRET) must decode to at least " + MIN_KEY_BYTES
                            + " bytes (HS256) — generate one with: openssl rand -base64 32");
        }
        return secret;
    }

    /**
     * Generates a signed JWT for the given user.
     * The token carries the user's UUID in the standard {@code sub} claim and the
     * role name in a {@code role} claim — nothing else identifies the caller.
     */
    public String generate(User user) {
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))                      // standard "sub" claim — the user UUID
                .claim(CLAIM_ROLE, user.getRole().getRoleName())            // e.g. "ADMIN" or "USER"
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Returns true if the token has a valid signature and is not expired.
     */
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Catches: expired, malformed, wrong signature, null token
            return false;
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public String extractRole(String token) {
        return parseClaims(token).get(CLAIM_ROLE, String.class);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
