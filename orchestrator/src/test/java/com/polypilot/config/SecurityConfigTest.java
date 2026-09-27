package com.polypilot.config;

import com.polypilot.auth.JwtFilter;
import com.polypilot.auth.controller.AuthController;
import com.polypilot.auth.entity.Role;
import com.polypilot.auth.entity.User;
import com.polypilot.auth.service.AuthService;
import com.polypilot.auth.service.JwtService;
import com.polypilot.auth.siwe.SiweController;
import com.polypilot.auth.siwe.SiweNonceService;
import com.polypilot.auth.siwe.SiweService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises {@link SecurityConfig}'s real {@code authorizeHttpRequests} rules end to end —
 * unlike the rest of the suite's {@code MockMvcBuilders.standaloneSetup(...)} pattern, which only wires {@link JwtFilter} directly and never touches the
 * actual authorization decision. That's the one thing this test needs to prove: GET
 * /api/auth/me requires a valid JWT while login/SIWE stay public, so a real
 * {@code SecurityFilterChain} bean is unavoidable — hence {@code @WebMvcTest} +
 * {@code @Import(SecurityConfig.class)} instead of the usual pattern.
 *
 * <p>The MockMvc instance is built explicitly via {@code webAppContextSetup(...)
 * .apply(springSecurity())} rather than the {@code @Autowired MockMvc} that
 * {@code @WebMvcTest} wires automatically — on this Boot 4.1 (Spring Security 7) app, the
 * auto-wired instance does not apply a request's own {@link JwtFilter}-set authentication to
 * the authorization decision (a valid Bearer token still gets a 403), even though the exact
 * same filter chain works correctly against the live app and in the explicit-builder form
 * used here. Root cause not isolated; flagged for anyone touching this test in the future.
 */
@WebMvcTest(controllers = {AuthController.class, SiweController.class})
@Import(SecurityConfig.class)
class SecurityConfigTest {

    /** 32 base64 bytes — a generated test-only value. */
    private static final String SECRET = "TCjcvvtjTRHefQlyrDJOratZz+IDf7cGtois83a7iLg=";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private SiweNonceService siweNonceService;

    @MockitoBean
    private SiweService siweService;

    private MockMvc mvc;

    @TestConfiguration
    static class Beans {
        @Bean
        JwtService jwtService() {
            return new JwtService(SECRET);
        }

        @Bean
        JwtFilter jwtFilter(JwtService jwtService) {
            return new JwtFilter(jwtService);
        }
    }

    @BeforeEach
    void buildMvcWithRealSecurityFilterChain() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
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
    void meWithoutTokenIsRejected() throws Exception {
        // Matches the status SecurityConfig actually returns today for every other
        // unauthenticated protected route (no custom AuthenticationEntryPoint configured).
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void meWithInvalidTokenIsRejected() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isForbidden());
    }

    @Test
    void meWithValidTokenSucceeds() throws Exception {
        mvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + tokenFor(UUID.randomUUID(), "USER")))
                .andExpect(status().isOk());
    }

    @Test
    void loginIsNotBlockedBySecurity() throws Exception {
        // AuthService is mocked, so reaching it (rather than being turned away by
        // SecurityConfig) is what a non-401/403 status here actually proves.
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"x\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void siweNonceIsNotBlockedBySecurity() throws Exception {
        mvc.perform(get("/api/auth/siwe/nonce"))
                .andExpect(status().isOk());
    }

    @Test
    void siweVerifyIsNotBlockedBySecurity() throws Exception {
        mvc.perform(post("/api/auth/siwe/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"m\",\"signature\":\"0xsig\"}"))
                .andExpect(status().isOk());
    }
}
