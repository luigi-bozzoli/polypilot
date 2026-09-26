package com.polypilot.auth.siwe;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * Canonical source of SIWE nonces for the SIWE login flow.
 *
 * <p>Nonces are cryptographically random, alphanumeric, and comfortably longer
 * than the 8-character EIP-4361 minimum. Each is stored in Redis under
 * {@code siwe-nonce:<nonce>} with a 5-minute TTL and is single-use: {@link #consume(String)}
 * deletes it atomically (Redis {@code GETDEL}), so a replay of the same nonce
 * finds nothing and fails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiweNonceService {

    static final String KEY_PREFIX = "siwe-nonce:";
    static final Duration TTL = Duration.ofMinutes(5);

    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int NONCE_LENGTH = 24; // >> EIP-4361 minimum of 8

    private final SecureRandom random = new SecureRandom();
    private final StringRedisTemplate redis;


    /** Generate a fresh nonce and store it with a 5-minute TTL. */
    public String issue() {
        String nonce = generate();
        redis.opsForValue().set(KEY_PREFIX + nonce, "1", TTL);
        return nonce;
    }

    /** True if the nonce is currently live (not yet consumed, not expired). */
    public boolean exists(String nonce) {
        return nonce != null && Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + nonce));
    }

    /**
     * Atomically consume the nonce. Returns {@code true} only if this call is the
     * one that removed a live nonce — concurrent callers and replays get {@code false}.
     */
    public boolean consume(String nonce) {
        if (nonce == null) {
            return false;
        }
        return redis.opsForValue().getAndDelete(KEY_PREFIX + nonce) != null;
    }

    private String generate() {
        StringBuilder sb = new StringBuilder(NONCE_LENGTH);
        for (int i = 0; i < NONCE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
