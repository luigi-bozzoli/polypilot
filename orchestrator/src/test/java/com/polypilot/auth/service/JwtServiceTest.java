package com.polypilot.auth.service;

import com.polypilot.auth.entity.Role;
import com.polypilot.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract of the JWT the rest of the auth flow depends on:
 * the user UUID travels in the standard {@code sub} claim, and nothing else
 * is needed to identify the caller.
 */
class JwtServiceTest {

    /** 32 base64 bytes — a generated test-only value, not the (now rejected) old dev default. */
    private static final String SECRET = "TCjcvvtjTRHefQlyrDJOratZz+IDf7cGtois83a7iLg=";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET);
    }

    private User user(UUID id, String roleName) {
        Role role = new Role();
        role.setRoleName(roleName);
        User u = new User();
        u.setId(id);
        u.setRole(role);
        return u;
    }

    private Claims rawClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Test
    void generatePutsUserUuidInSubClaim() {
        UUID id = UUID.randomUUID();

        String token = jwtService.generate(user(id, "USER"));

        assertThat(rawClaims(token).getSubject()).isEqualTo(id.toString());
    }

    @Test
    void extractUserIdReturnsTheSubClaimAsUuid() {
        UUID id = UUID.randomUUID();

        String token = jwtService.generate(user(id, "ADMIN"));

        assertThat(jwtService.extractUserId(token)).isEqualTo(id);
    }

    @Test
    void roleClaimIsPreserved() {
        String token = jwtService.generate(user(UUID.randomUUID(), "ADMIN"));

        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void tokenDoesNotCarryRedundantUserIdClaim() {
        UUID id = UUID.randomUUID();

        String token = jwtService.generate(user(id, "USER"));

        // sub already carries the identity; a second "userId" claim is dead weight
        // and no code in the monorepo reads it.
        assertThat(rawClaims(token).get("userId")).isNull();
    }

    /**
     * {@code jwt.secret} must fail fast at startup rather than silently accept a
     * value that would let anyone forge tokens.
     */
    @Nested
    class SecretValidation {

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void rejectsMissingOrBlank(String secret) {
            assertThatThrownBy(() -> new JwtService(secret))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must be set");
        }

        @Test
        void rejectsNull() {
            assertThatThrownBy(() -> new JwtService(null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must be set");
        }

        @Test
        void rejectsTooShort() {
            // 16 raw bytes, base64-encoded — valid base64, under the 32-byte HS256 floor.
            String tooShort = "MTIzNDU2Nzg5MDEyMzQ1Ng==";

            assertThatThrownBy(() -> new JwtService(tooShort))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("32");
        }

        @Test
        void rejectsInvalidBase64() {
            assertThatThrownBy(() -> new JwtService("not-valid-base64!!!"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("base64");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv72ol/pe/Unols=",
                "change-me", "CHANGE-ME", "changeme", "replace_me", "REPLACEME", "secret", "Secret",
        })
        void rejectsDenylistedValues(String secret) {
            assertThatThrownBy(() -> new JwtService(secret))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("default or placeholder");
        }

        @Test
        void acceptsAGeneratedSecret() {
            assertThat(new JwtService(SECRET)).isNotNull();
        }
    }
}
