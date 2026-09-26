package com.polypilot.auth.siwe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reply from the auth-service SIWE verification endpoint: the checksummed
 * Ethereum address recovered from the signature. The auth-service does signature
 * recovery only — every SIWE policy check lives here in the orchestrator.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiweVerifyResponse {
    String address;
}
