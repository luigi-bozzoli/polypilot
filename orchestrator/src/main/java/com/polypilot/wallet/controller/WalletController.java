package com.polypilot.wallet.controller;

import com.polypilot.wallet.dto.WalletConnectRequest;
import com.polypilot.wallet.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * One endpoint, requires a valid JWT (enforced by Spring Security).
 *
 * POST /api/wallet/connect → submit a signed ClobAuth message
 *
 * <p>There is no wallet-challenge round-trip: SIWE login already established
 * ownership of the wallet address, and the JWT is the proof of it.
 */
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Called after the user has signed the ClobAuth message in their wallet.
     * On success, the L2 credentials are derived and stored and the bot can trade.
     *
     * @param request  { address, signature, timestamp, nonce } — timestamp and
     *                 nonce come from the client's ClobAuth message
     * @param userId   the authenticated user, from the JWT {@code sub} claim
     */
    @PostMapping("/connect")
    public ResponseEntity<Void> connect(
            @RequestBody WalletConnectRequest request,
            @AuthenticationPrincipal UUID userId
    ) {
        walletService.connectWallet(request, userId);
        return ResponseEntity.ok().build();
    }
}
