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
import com.polypilot.wallet.client.AuthServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SiweServiceTest {

    private static final String NONCE = "abc12345def";
    private static final String ADDRESS_LOWER = SiweTestMessages.ADDRESS.toLowerCase();

    @Mock AuthServiceClient authServiceClient;
    @Mock SiweNonceService nonceService;
    @Mock UserRepository userRepository;
    @Mock UserIdentityRepository identityRepository;
    @Mock RoleRepository roleRepository;
    @Mock JwtService jwtService;

    SiweProperties properties;
    SiweService service;

    @BeforeEach
    void setUp() {
        properties = new SiweProperties();
        properties.setDomain(SiweTestMessages.DOMAIN);
        properties.setUri(SiweTestMessages.URI);
        properties.setAllowedChainIds(List.of(1L, 137L));

        service = new SiweService(properties, nonceService, authServiceClient,
                userRepository, identityRepository, roleRepository, jwtService);

        // Defaults for the happy path — individual tests override as needed.
        when(nonceService.exists(NONCE)).thenReturn(true);
        when(nonceService.consume(NONCE)).thenReturn(true);
        when(authServiceClient.verifySiwe(anyString(), anyString()))
                .thenReturn(new SiweVerifyResponse(SiweTestMessages.ADDRESS));
        when(identityRepository.findByTypeAndIdentifier(IdentityType.ETH_WALLET, ADDRESS_LOWER))
                .thenReturn(Optional.empty());
        Role userRole = new Role();
        userRole.setRoleName("USER");
        when(roleRepository.findByRoleName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(identityRepository.save(any(UserIdentity.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generate(any(User.class))).thenReturn("jwt-token");
    }

    private String validMessage() {
        return SiweTestMessages.builder().nonce(NONCE).build();
    }

    private void assertUnauthorized(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    // ── happy paths ──────────────────────────────────────────────────────────

    @Test
    void newWalletCreatesUserWithUserRoleAndLowercaseIdentity() {
        LoginResponse res = service.verify(validMessage(), "0xsig");

        assertThat(res.getToken()).isEqualTo("jwt-token");
        assertThat(res.getRole()).isEqualTo("USER");
        assertThat(res.getEmail()).isEqualTo(ADDRESS_LOWER);

        ArgumentCaptor<UserIdentity> identity = ArgumentCaptor.forClass(UserIdentity.class);
        verify(identityRepository).save(identity.capture());
        UserIdentity saved = identity.getValue();
        assertThat(saved.getType()).isEqualTo(IdentityType.ETH_WALLET);
        assertThat(saved.getIdentifier()).isEqualTo(ADDRESS_LOWER);
        assertThat(saved.getSecret()).isNull();
        assertThat(saved.getVerifiedAt()).isNotNull();
        assertThat(saved.getLastUsedAt()).isNotNull();
        assertThat(saved.getUser().getRole().getRoleName()).isEqualTo("USER");

        verify(userRepository).save(any(User.class));
        verify(nonceService).consume(NONCE);
    }

    @Test
    void existingWalletLogsInWithoutCreatingDuplicate() {
        User existing = new User();
        Role role = new Role();
        role.setRoleName("USER");
        existing.setRole(role);
        existing.setId(UUID.randomUUID());
        UserIdentity identity = new UserIdentity();
        identity.setType(IdentityType.ETH_WALLET);
        identity.setIdentifier(ADDRESS_LOWER);
        identity.setUser(existing);
        when(identityRepository.findByTypeAndIdentifier(IdentityType.ETH_WALLET, ADDRESS_LOWER))
                .thenReturn(Optional.of(identity));

        LoginResponse res = service.verify(validMessage(), "0xsig");

        assertThat(res.getToken()).isEqualTo("jwt-token");
        verify(userRepository, never()).save(any());
        ArgumentCaptor<UserIdentity> saved = ArgumentCaptor.forClass(UserIdentity.class);
        verify(identityRepository).save(saved.capture());
        assertThat(saved.getValue().getLastUsedAt()).isNotNull();
    }

    // ── 401 matrix ───────────────────────────────────────────────────────────

    @Test
    void rejectsMalformedMessage() {
        assertUnauthorized(() -> service.verify("not a siwe message", "0xsig"));
    }

    @Test
    void rejectsWrongDomain() {
        String msg = SiweTestMessages.builder().nonce(NONCE).domain("evil.example").build();
        assertUnauthorized(() -> service.verify(msg, "0xsig"));
    }

    @Test
    void rejectsWrongUri() {
        String msg = SiweTestMessages.builder().nonce(NONCE).uri("http://evil.example").build();
        assertUnauthorized(() -> service.verify(msg, "0xsig"));
    }

    @Test
    void rejectsUnknownNonce() {
        when(nonceService.exists(NONCE)).thenReturn(false);
        assertUnauthorized(() -> service.verify(validMessage(), "0xsig"));
    }

    @Test
    void rejectsFutureIssuedAt() {
        String msg = SiweTestMessages.builder().nonce(NONCE)
                .issuedAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(1)).build();
        assertUnauthorized(() -> service.verify(msg, "0xsig"));
    }

    @Test
    void rejectsStaleIssuedAt() {
        String msg = SiweTestMessages.builder().nonce(NONCE)
                .issuedAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1)).build();
        assertUnauthorized(() -> service.verify(msg, "0xsig"));
    }

    @Test
    void rejectsExpiredExpirationTime() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String msg = SiweTestMessages.builder().nonce(NONCE)
                .issuedAt(now.minusMinutes(5))
                .expirationTime(now.minusMinutes(2))
                .build();
        assertUnauthorized(() -> service.verify(msg, "0xsig"));
    }

    @Test
    void rejectsUnsupportedVersion() {
        String msg = SiweTestMessages.builder().nonce(NONCE).version("2").build();
        assertUnauthorized(() -> service.verify(msg, "0xsig"));
    }

    @Test
    void rejectsMessageAddressNotEip55Checksummed() {
        // Recovery succeeds and returns the EIP-55 checksummed address, but the
        // message carried an all-lowercase address — EIP-4361 requires EIP-55.
        String msg = SiweTestMessages.builder().nonce(NONCE)
                .address(SiweTestMessages.ADDRESS.toLowerCase())
                .build();
        assertUnauthorized(() -> service.verify(msg, "0xsig"));
    }

    @Test
    void rejectsDisallowedChainId() {
        String msg = SiweTestMessages.builder().nonce(NONCE).chainId(999).build();
        assertUnauthorized(() -> service.verify(msg, "0xsig"));
    }

    @Test
    void rejectsWhenAuthServiceCannotRecoverSignature() {
        when(authServiceClient.verifySiwe(anyString(), anyString()))
                .thenThrow(new RuntimeException("400 Bad Request"));
        assertUnauthorized(() -> service.verify(validMessage(), "0xbad"));
    }

    @Test
    void rejectsWhenRecoveredAddressDiffersFromMessage() {
        when(authServiceClient.verifySiwe(anyString(), anyString()))
                .thenReturn(new SiweVerifyResponse("0x0000000000000000000000000000000000000001"));
        assertUnauthorized(() -> service.verify(validMessage(), "0xsig"));
    }

    @Test
    void rejectsReusedNonce() {
        // passes the existence check but loses the atomic consume race
        when(nonceService.exists(NONCE)).thenReturn(true);
        when(nonceService.consume(NONCE)).thenReturn(false);
        assertUnauthorized(() -> service.verify(validMessage(), "0xsig"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void doesNotConsumeNonceBeforeSignatureCheck() {
        when(authServiceClient.verifySiwe(anyString(), anyString()))
                .thenThrow(new RuntimeException("bad sig"));
        assertUnauthorized(() -> service.verify(validMessage(), "0xbad"));
        verify(nonceService, never()).consume(anyString());
    }
}
