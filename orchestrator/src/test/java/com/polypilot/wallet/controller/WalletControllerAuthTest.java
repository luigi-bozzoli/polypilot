package com.polypilot.wallet.controller;

import com.polypilot.auth.JwtFilter;
import com.polypilot.auth.entity.Role;
import com.polypilot.auth.entity.User;
import com.polypilot.auth.service.JwtService;
import com.polypilot.wallet.dto.WalletConnectRequest;
import com.polypilot.wallet.service.WalletService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives {@link WalletController} through the <em>real</em> {@link JwtFilter} and
 * the real {@code @AuthenticationPrincipal} argument resolver, so the UUID that
 * lands in {@code connect(..., UUID userId)} is the one Spring Security recovered
 * from an actual JWT — not a value stubbed straight into the controller.
 */
class WalletControllerAuthTest {

    /** 32 base64 bytes — a generated test-only value, not the (now rejected) old dev default. */
    private static final String SECRET = "TCjcvvtjTRHefQlyrDJOratZz+IDf7cGtois83a7iLg=";
    private static final String BODY =
            "{\"address\":\"0xabc\",\"signature\":\"0xsig\",\"timestamp\":\"123\",\"nonce\":\"0\"}";

    private WalletService walletService;
    private JwtService jwtService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        walletService = mock(WalletService.class);
        jwtService = new JwtService(SECRET);

        mvc = MockMvcBuilders.standaloneSetup(new WalletController(walletService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .addFilters(new JwtFilter(jwtService))
                .build();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private String tokenFor(UUID id, String role) {
        Role r = new Role();
        r.setRoleName(role);
        User u = new User();
        u.setId(id);
        u.setRole(r);
        return jwtService.generate(u);
    }

    @Test
    void authenticationPrincipalResolvesToTheJwtSubUuid() throws Exception {
        UUID userId = UUID.randomUUID();

        mvc.perform(post("/api/wallet/connect")
                        .header("Authorization", "Bearer " + tokenFor(userId, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk());

        ArgumentCaptor<UUID> captor = ArgumentCaptor.forClass(UUID.class);
        verify(walletService).connectWallet(any(WalletConnectRequest.class), captor.capture());
        assertThat(captor.getValue())
                .as("@AuthenticationPrincipal UUID must be the JWT sub claim")
                .isEqualTo(userId);
    }

    @Test
    void distinctTokensYieldDistinctPrincipals() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        mvc.perform(post("/api/wallet/connect")
                        .header("Authorization", "Bearer " + tokenFor(first, "USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());
        SecurityContextHolder.clearContext();
        mvc.perform(post("/api/wallet/connect")
                        .header("Authorization", "Bearer " + tokenFor(second, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());

        ArgumentCaptor<UUID> captor = ArgumentCaptor.forClass(UUID.class);
        verify(walletService, org.mockito.Mockito.times(2))
                .connectWallet(any(WalletConnectRequest.class), captor.capture());
        assertThat(captor.getAllValues()).containsExactly(first, second);
    }
}
