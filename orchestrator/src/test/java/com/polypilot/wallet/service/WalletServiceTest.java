package com.polypilot.wallet.service;

import com.polypilot.auth.entity.UserIdentity;
import com.polypilot.auth.enums.IdentityType;
import com.polypilot.auth.repository.UserIdentityRepository;
import com.polypilot.wallet.client.AuthServiceClient;
import com.polypilot.wallet.dto.L2Credentials;
import com.polypilot.wallet.dto.WalletConnectRequest;
import com.polypilot.wallet.entity.WalletCredential;
import com.polypilot.wallet.repository.WalletCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link WalletService#connectWallet} after the wallet-challenge flow was removed.
 *
 * <p>The contract under test:
 * <ul>
 *   <li>no Redis challenge is issued, read, validated or deleted;</li>
 *   <li>the authenticated {@code userId} (JWT {@code sub}) identifies the caller;</li>
 *   <li>the request address must match the caller's {@code ETH_WALLET}
 *       {@link UserIdentity} (case-insensitive) or the call is rejected before any
 *       credential is derived or persisted;</li>
 *   <li>credentials are still persisted encrypted through {@link EncryptionService}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String WALLET = "0xabc0000000000000000000000000000000000abc";

    @Mock AuthServiceClient authServiceClient;
    @Mock EncryptionService encryption;
    @Mock WalletCredentialRepository repository;
    @Mock UserIdentityRepository identityRepository;

    WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(authServiceClient, encryption, repository, identityRepository);
    }

    private static WalletConnectRequest request(String address) {
        // timestamp + nonce are now produced by the client as part of the ClobAuth
        // message it sends to Polymarket — the orchestrator only forwards them.
        return new WalletConnectRequest(address, "0xsignature", "1735689600", "17");
    }

    private static UserIdentity walletIdentity(String identifierLower) {
        UserIdentity identity = new UserIdentity();
        identity.setType(IdentityType.ETH_WALLET);
        identity.setIdentifier(identifierLower);
        return identity;
    }

    private static UserIdentity passwordIdentity() {
        UserIdentity identity = new UserIdentity();
        identity.setType(IdentityType.PASSWORD);
        identity.setIdentifier("demo@polypilot.local");
        return identity;
    }

    @Test
    void connectsWithoutAnyPriorChallengeWhenAddressMatchesAuthenticatedIdentity() {
        when(identityRepository.findByUserId(USER_ID))
                .thenReturn(List.of(walletIdentity(WALLET)));
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(authServiceClient.verifyAndDerive(any(WalletConnectRequest.class)))
                .thenReturn(new L2Credentials("k", "s", "p"));
        when(encryption.encrypt("k")).thenReturn("k-enc");
        when(encryption.encrypt("s")).thenReturn("s-enc");
        when(encryption.encrypt("p")).thenReturn("p-enc");

        walletService.connectWallet(request(WALLET), USER_ID);

        verify(authServiceClient).verifyAndDerive(any(WalletConnectRequest.class));
        verify(repository).save(any(WalletCredential.class));
    }

    @Test
    void addressMatchIsCaseInsensitive() {
        when(identityRepository.findByUserId(USER_ID))
                .thenReturn(List.of(walletIdentity(WALLET))); // stored lower-case
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(authServiceClient.verifyAndDerive(any(WalletConnectRequest.class)))
                .thenReturn(new L2Credentials("k", "s", "p"));
        when(encryption.encrypt(any())).thenReturn("enc");

        walletService.connectWallet(request(WALLET.toUpperCase()), USER_ID);

        verify(repository).save(any(WalletCredential.class));
    }

    @Test
    void rejectsAddressThatDoesNotBelongToTheAuthenticatedUser() {
        when(identityRepository.findByUserId(USER_ID))
                .thenReturn(List.of(walletIdentity("0xdead000000000000000000000000000000000dead")));

        assertThatThrownBy(() -> walletService.connectWallet(request(WALLET), USER_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        // defence in depth: nothing is derived or persisted for a mismatched wallet
        verifyNoInteractions(authServiceClient);
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsWhenTheAuthenticatedUserHasNoWalletIdentity() {
        when(identityRepository.findByUserId(USER_ID)).thenReturn(List.of(passwordIdentity()));

        assertThatThrownBy(() -> walletService.connectWallet(request(WALLET), USER_ID))
                .isInstanceOf(ResponseStatusException.class);

        verifyNoInteractions(authServiceClient);
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsWhenTheJwtSubjectResolvesToNoIdentityAtAll() {
        when(identityRepository.findByUserId(USER_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> walletService.connectWallet(request(WALLET), USER_ID))
                .isInstanceOf(ResponseStatusException.class);

        verifyNoInteractions(authServiceClient);
        verify(repository, never()).save(any());
    }

    @Test
    void persistsCredentialsEncryptedThroughEncryptionService() {
        when(identityRepository.findByUserId(USER_ID))
                .thenReturn(List.of(walletIdentity(WALLET)));
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(authServiceClient.verifyAndDerive(any(WalletConnectRequest.class)))
                .thenReturn(new L2Credentials("api-key-plain", "secret-plain", "passphrase-plain"));
        when(encryption.encrypt("api-key-plain")).thenReturn("api-key-cipher");
        when(encryption.encrypt("secret-plain")).thenReturn("secret-cipher");
        when(encryption.encrypt("passphrase-plain")).thenReturn("passphrase-cipher");

        walletService.connectWallet(request(WALLET), USER_ID);

        ArgumentCaptor<WalletCredential> saved = ArgumentCaptor.forClass(WalletCredential.class);
        verify(repository).save(saved.capture());
        WalletCredential row = saved.getValue();
        assertThat(row.getUserId()).isEqualTo(USER_ID);
        assertThat(row.getWalletAddress()).isEqualTo(WALLET);
        assertThat(row.getApiKeyEnc()).isEqualTo("api-key-cipher");
        assertThat(row.getSecretEnc()).isEqualTo("secret-cipher");
        assertThat(row.getPassphraseEnc()).isEqualTo("passphrase-cipher");
        // no plaintext leaks into the persisted row
        assertThat(row.getApiKeyEnc()).isNotEqualTo("api-key-plain");
    }

    @Test
    void serviceHoldsNoRedisChallengeCollaborator() {
        boolean hasRedisField = Arrays.stream(WalletService.class.getDeclaredFields())
                .map(Field::getType)
                .map(Class::getName)
                .anyMatch(name -> name.contains("Redis"));
        assertThat(hasRedisField)
                .as("WalletService must not depend on Redis once the challenge flow is gone")
                .isFalse();
    }
}
