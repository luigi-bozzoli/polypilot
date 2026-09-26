package com.polypilot.auth.siwe;

import com.polypilot.auth.dto.LoginResponse;
import com.polypilot.auth.siwe.dto.NonceResponse;
import com.polypilot.auth.siwe.dto.SiweVerifyRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * SIWE (EIP-4361) login endpoints. Both are public — explicitly permitted in
 * {@code SecurityConfig} (not via a blanket {@code /api/auth/**} rule, which would
 * also expose {@code GET /api/auth/me}).
 *
 * <pre>
 * GET  /api/auth/siwe/nonce    → { "nonce": "..." }
 * POST /api/auth/siwe/verify   → standard LoginResponse (JWT) on success, 401 otherwise
 * </pre>
 */
@RestController
@RequestMapping("/api/auth/siwe")
public class SiweController {

    private final SiweNonceService nonceService;
    private final SiweService siweService;

    public SiweController(SiweNonceService nonceService, SiweService siweService) {
        this.nonceService = nonceService;
        this.siweService = siweService;
    }

    @GetMapping("/nonce")
    public NonceResponse nonce() {
        return new NonceResponse(nonceService.issue());
    }

    @PostMapping("/verify")
    public LoginResponse verify(@Valid @RequestBody SiweVerifyRequest request) {
        return siweService.verify(request.getMessage(), request.getSignature());
    }
}
