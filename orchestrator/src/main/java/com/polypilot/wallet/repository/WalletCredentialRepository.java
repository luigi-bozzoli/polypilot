package com.polypilot.wallet.repository;

import com.polypilot.wallet.entity.WalletCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface WalletCredentialRepository extends JpaRepository<WalletCredential, UUID> {

    Optional<WalletCredential> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}