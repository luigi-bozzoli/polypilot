package com.polypilot.wallet.service;

import com.polypilot.common.exception.EncryptionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption for wallet credentials at rest.
 *
 * Why GCM over CBC:
 *  - Authenticated encryption: detects tampering (ciphertext + tag).
 *  - No padding oracle attacks.
 *  - Each encrypt call produces a unique IV — safe to reuse the same key.
 *
 * The key is loaded from ENCRYPTION_KEY env var (32 bytes, base64-encoded).
 * Generate one with: openssl rand -base64 32
 */
@Service
public class EncryptionService {

    private static final String ALGORITHM  = "AES/GCM/NoPadding";
    private static final int    GCM_IV_LEN = 12;   // bytes — NIST recommended
    private static final int    GCM_TAG_LEN = 128;  // bits

    private final SecretKeySpec secretKey;
    private final SecureRandom  random = new SecureRandom();

    public EncryptionService(@Value("${encryption.key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("Encryption key must be 32 bytes (AES-256)");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypts plaintext. Output format: base64(IV || ciphertext+tag).
     * The IV is prepended so decrypt() can extract it without a separate store.
     */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LEN];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LEN, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

            // prepend IV to ciphertext so we can recover it on decryption
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new EncryptionException("Encryption failed", e);
        }
    }

    /**
     * Decrypts a value produced by encrypt(). Extracts the IV from the prefix.
     */
    public String decrypt(String base64Ciphertext) {
        try {
            byte[] combined = Base64.getDecoder().decode(base64Ciphertext);

            byte[] iv         = new byte[GCM_IV_LEN];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LEN];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LEN);
            System.arraycopy(combined, GCM_IV_LEN, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LEN, iv));

            return new String(cipher.doFinal(ciphertext));
        } catch (Exception e) {
            throw new EncryptionException("Decryption failed", e);
        }
    }
}