package com.polypilot.wallet.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallet_credentials")
@NoArgsConstructor
@Getter
@Setter
public class WalletCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "wallet_address", nullable = false)
    private String walletAddress;

    @Column(name = "api_key_enc", nullable = false)
    private String apiKeyEnc;

    @Column(name = "secret_enc", nullable = false)
    private String secretEnc;

    @Column(name = "passphrase_enc", nullable = false)
    private String passphraseEnc;

    @Column(name = "connected_at", nullable = false)
    private Instant connectedAt = Instant.now();

}