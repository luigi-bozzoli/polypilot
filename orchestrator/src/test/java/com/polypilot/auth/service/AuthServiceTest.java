package com.polypilot.auth.service;

import com.polypilot.auth.dto.LoginRequest;
import com.polypilot.auth.dto.LoginResponse;
import com.polypilot.auth.entity.Role;
import com.polypilot.auth.entity.User;
import com.polypilot.auth.entity.UserIdentity;
import com.polypilot.auth.enums.IdentityType;
import com.polypilot.auth.repository.UserIdentityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserIdentityRepository identityRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(identityRepository, passwordEncoder, jwtService);
    }

    private UserIdentity passwordIdentity(String hash) {
        Role role = new Role();
        role.setRoleName("USER");
        User user = new User();
        user.setRole(role);
        UserIdentity identity = new UserIdentity();
        identity.setType(IdentityType.PASSWORD);
        identity.setIdentifier("demo@polypilot.local");
        identity.setSecret(hash);
        identity.setUser(user);
        return identity;
    }

    @Test
    void looksUpPasswordIdentityByLowercasedEmail() {
        UserIdentity identity = passwordIdentity("$2y$12$hash");
        when(identityRepository.findByTypeAndIdentifier(IdentityType.PASSWORD, "demo@polypilot.local"))
                .thenReturn(Optional.of(identity));
        when(passwordEncoder.matches("demo1234", "$2y$12$hash")).thenReturn(true);
        when(jwtService.generate(any(User.class))).thenReturn("jwt-token");

        LoginResponse res = authService.login(new LoginRequest("Demo@PolyPilot.Local", "demo1234"));

        assertThat(res.getToken()).isEqualTo("jwt-token");
        assertThat(res.getEmail()).isEqualTo("demo@polypilot.local");
        assertThat(res.getRole()).isEqualTo("USER");
        verify(identityRepository).findByTypeAndIdentifier(IdentityType.PASSWORD, "demo@polypilot.local");
        verify(identityRepository).save(identity); // last_used_at bump
    }

    @Test
    void unknownEmailAndWrongPasswordYieldIdentical401() {
        // unknown identifier
        when(identityRepository.findByTypeAndIdentifier(IdentityType.PASSWORD, "ghost@polypilot.local"))
                .thenReturn(Optional.empty());
        ResponseStatusException unknown = catchLogin("ghost@polypilot.local", "whatever");

        // known identifier, wrong password
        UserIdentity identity = passwordIdentity("$2y$12$hash");
        when(identityRepository.findByTypeAndIdentifier(IdentityType.PASSWORD, "demo@polypilot.local"))
                .thenReturn(Optional.of(identity));
        lenient().when(passwordEncoder.matches("nope", "$2y$12$hash")).thenReturn(false);
        ResponseStatusException wrongPass = catchLogin("demo@polypilot.local", "nope");

        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrongPass.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrongPass.getReason()).isEqualTo(unknown.getReason());
    }

    @Test
    void passwordIdentityWithNullSecretIsRejected() {
        when(identityRepository.findByTypeAndIdentifier(IdentityType.PASSWORD, "demo@polypilot.local"))
                .thenReturn(Optional.of(passwordIdentity(null)));
        assertThat(catchLogin("demo@polypilot.local", "demo1234").getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ResponseStatusException catchLogin(String email, String password) {
        try {
            authService.login(new LoginRequest(email, password));
            throw new AssertionError("expected login to fail with 401");
        } catch (ResponseStatusException e) {
            return e;
        }
    }
}
