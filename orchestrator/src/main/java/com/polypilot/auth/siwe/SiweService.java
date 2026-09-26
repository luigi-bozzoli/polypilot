package com.polypilot.auth.siwe;

import com.polypilot.auth.dto.LoginResponse;
import com.polypilot.auth.entity.Role;
import com.polypilot.auth.entity.User;
import com.polypilot.auth.entity.UserIdentity;
import com.polypilot.auth.enums.IdentityType;
import com.polypilot.auth.repository.RoleRepository;
import com.polypilot.auth.repository.UserIdentityRepository;
import com.polypilot.auth.repository.UserRepository;
import com.polypilot.auth.service.JwtService;
import com.polypilot.auth.siwe.dto.SiweVerifyResponse;
import com.polypilot.auth.siwe.enums.SiweRejectionReason;
import com.polypilot.wallet.client.AuthServiceClient;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * SIWE (EIP-4361) login. The orchestrator owns every SIWE policy check; the
 * auth-service only recovers the signing address.
 *
 * <p>Flow of {@link #verify(String, String)}:
 * <ol>
 *   <li>parse the raw message,</li>
 *   <li>validate domain / URI / nonce presence / issuedAt / expirationTime / chainId,</li>
 *   <li>recover the address via {@link AuthServiceClient#verifySiwe} and match it
 *       to the message, exact-case, so a valid EIP-55 checksum is also required,</li>
 *   <li>atomically consume the nonce (replay after this point fails),</li>
 *   <li>find-or-create the {@code ETH_WALLET} identity (address stored lower-case),</li>
 *   <li>issue a JWT and return the standard {@link LoginResponse}.</li>
 * </ol>
 * Any authentication failure surfaces as {@code 401} with an identical, non-revealing message.
 */
@Slf4j
@Service
@AllArgsConstructor
public class SiweService {

    private static final String DEFAULT_ROLE = "USER";
    private static final Duration CLOCK_SKEW = Duration.ofSeconds(60);
    private static final Duration MAX_ISSUED_AT_AGE = Duration.ofMinutes(10);

    private final SiweProperties properties;
    private final SiweNonceService nonceService;
    private final AuthServiceClient authServiceClient;
    private final UserRepository userRepository;
    private final UserIdentityRepository identityRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;

    @PostConstruct
    void logConfiguration() {
        log.info(
                "SIWE policy: domain=[{}] uri=[{}] allowedChainIds={}",
                properties.getDomain(),
                properties.getUri(),
                properties.getAllowedChainIds()
        );
    }

    @Transactional
    public LoginResponse verify(String message, String signature) {
        SiweMessage siwe = parse(message);
        validatePolicy(siwe);
        verifySignature(siwe, message, signature);
        consumeNonce(siwe.getNonce());

        String addressLower = siwe.getAddress().toLowerCase(Locale.ROOT);
        User user = findOrCreateWalletUser(addressLower);

        String token = jwtService.generate(user);
        return new LoginResponse(token, addressLower, user.getRole().getRoleName());
    }

    // ── steps ────────────────────────────────────────────────────────────────

    private SiweMessage parse(String message) {
        try {
            return SiweMessage.parse(message);
        } catch (IllegalArgumentException ex) {
            log.debug("Rejected SIWE login: unparseable message [{}]", ex.getMessage());
            throw unauthorized();
        }
    }

    private void validatePolicy(SiweMessage siwe) {
        if (!siwe.getDomain().equals(properties.getDomain())) {
            reject(SiweRejectionReason.DOMAIN_MISMATCH);
        }
        if (!siwe.getUri().equals(properties.getUri())) {
            reject(SiweRejectionReason.URI_MISMATCH);
        }
        if (!nonceService.exists(siwe.getNonce())) {
            reject(SiweRejectionReason.UNKNOWN_OR_EXPIRED_NONCE);
        }
        validateTimes(siwe);
        if (!properties.getAllowedChainIds().contains(siwe.getChainId())) {
            reject(SiweRejectionReason.CHAIN_ID_NOT_ALLOWED, siwe.getChainId());
        }
    }

    private void validateTimes(SiweMessage siwe) {
        OffsetDateTime now = OffsetDateTime.now();

        OffsetDateTime issuedAt = siwe.getIssuedAt();
        if (issuedAt == null) {
            reject(SiweRejectionReason.MISSING_ISSUED_AT);
        }
        if (issuedAt.isAfter(now.plus(CLOCK_SKEW))) {
            reject(SiweRejectionReason.ISSUED_AT_IN_FUTURE);
        }
        if (issuedAt.isBefore(now.minus(MAX_ISSUED_AT_AGE))) {
            reject(SiweRejectionReason.ISSUED_AT_TOO_OLD);
        }

        OffsetDateTime notBefore = siwe.getNotBefore();
        if (notBefore != null && now.plus(CLOCK_SKEW).isBefore(notBefore)) {
            reject(SiweRejectionReason.NOT_YET_VALID);
        }

        OffsetDateTime expiration = siwe.getExpirationTime();
        if (expiration != null) {
            if (!expiration.isAfter(issuedAt)) {
                reject(SiweRejectionReason.EXPIRATION_PRECEDES_ISSUED_AT);
            }
            if (now.minus(CLOCK_SKEW).isAfter(expiration)) {
                reject(SiweRejectionReason.MESSAGE_EXPIRED);
            }
        }
    }

    private void verifySignature(SiweMessage siwe, String message, String signature) {
        SiweVerifyResponse recovered;
        try {
            recovered = authServiceClient.verifySiwe(message, signature);
        } catch (RuntimeException ex) {
            log.debug("Rejected SIWE login: auth-service could not recover signature [{}]", ex.getMessage());
            throw unauthorized();
        }
        if (recovered == null || recovered.getAddress() == null) {
            reject(SiweRejectionReason.NO_ADDRESS_RECOVERED);
        }
        // Exact-case comparison: the auth-service returns an EIP-55 checksummed
        // address, so a match also proves the message's address field carried a
        // valid EIP-55 checksum (EIP-4361 requires it), not merely the right bytes.
        if (!recovered.getAddress().equals(siwe.getAddress())) {
            reject(SiweRejectionReason.ADDRESS_MISMATCH);
        }
    }

    private void consumeNonce(String nonce) {
        if (!nonceService.consume(nonce)) {
            // Lost a race, or the nonce was already spent between the existence
            // check and here — treat as replay.
            reject(SiweRejectionReason.NONCE_ALREADY_CONSUMED);
        }
    }

    private User findOrCreateWalletUser(String addressLower) {
        UserIdentity identity = identityRepository
                .findByTypeAndIdentifier(IdentityType.ETH_WALLET, addressLower)
                .orElse(null);

        if (identity != null) {
            identity.setLastUsedAt(OffsetDateTime.now());
            identityRepository.save(identity);
            return identity.getUser();
        }

        Role role = roleRepository.findByRoleName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException("Default role " + DEFAULT_ROLE + " not found"));

        User user = new User();
        user.setRole(role);
        user.setDisplayName(addressLower);
        userRepository.save(user);

        UserIdentity created = new UserIdentity();
        created.setUser(user);
        created.setType(IdentityType.ETH_WALLET);
        created.setIdentifier(addressLower);
        created.setSecret(null); // the SIWE signature is the proof — no stored secret
        created.setVerifiedAt(OffsetDateTime.now());
        created.setLastUsedAt(OffsetDateTime.now());
        identityRepository.save(created);

        log.info("Created wallet user for address [{}]", addressLower);
        return user;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void reject(SiweRejectionReason reason, Object... args) {
        log.debug("Rejected SIWE login: {}", reason.format(args));
        throw unauthorized();
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SIWE authentication failed");
    }
}