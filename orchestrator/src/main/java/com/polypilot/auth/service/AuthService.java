package com.polypilot.auth.service;

import com.polypilot.auth.dto.LoginRequest;
import com.polypilot.auth.dto.LoginResponse;
import com.polypilot.auth.entity.User;
import com.polypilot.auth.entity.UserIdentity;
import com.polypilot.auth.enums.IdentityType;
import com.polypilot.auth.repository.UserIdentityRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * Password login against {@code user_identities}. Password accounts are
 * seeded / admin-managed — there is no self-service registration.
 */
@Service
@AllArgsConstructor
public class AuthService {

    private final UserIdentityRepository identityRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase(Locale.ROOT);

        // Identical 401 for unknown identifier, missing secret, and wrong password
        // — never reveal which credential failed (no user enumeration).
        UserIdentity identity = identityRepository
                .findByTypeAndIdentifier(IdentityType.PASSWORD, email)
                .orElse(null);

        if (identity == null
                || identity.getSecret() == null
                || !passwordEncoder.matches(request.getPassword(), identity.getSecret())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        User user = identity.getUser();

        identity.setLastUsedAt(OffsetDateTime.now());
        identityRepository.save(identity);

        String token = jwtService.generate(user);

        return new LoginResponse(token, email, user.getRole().getRoleName());
    }
}
