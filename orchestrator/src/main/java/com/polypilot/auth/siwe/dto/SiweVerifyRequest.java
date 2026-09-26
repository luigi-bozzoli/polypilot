package com.polypilot.auth.siwe.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body of {@code POST /api/auth/siwe/verify}, and — same shape — the payload the
 * orchestrator forwards to the auth-service {@code POST /auth/siwe/verify}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiweVerifyRequest {

    /** The exact EIP-4361 message string that was signed. */
    @NotBlank
    String message;

    /** {@code 0x…} signature produced by {@code personal_sign}. */
    @NotBlank
    String signature;
}
