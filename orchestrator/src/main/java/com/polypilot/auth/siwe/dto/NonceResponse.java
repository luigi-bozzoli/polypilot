package com.polypilot.auth.siwe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body of {@code GET /api/auth/siwe/nonce}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NonceResponse {
    String nonce;
}
