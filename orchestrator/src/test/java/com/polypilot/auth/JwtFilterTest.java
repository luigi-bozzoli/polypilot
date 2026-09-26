package com.polypilot.auth;

import com.polypilot.auth.entity.Role;
import com.polypilot.auth.entity.User;
import com.polypilot.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * {@link JwtFilter} must populate the {@code SecurityContext} with the caller's
 * UUID (the JWT {@code sub}) as the principal — never the email string — while
 * still deriving the {@code ROLE_<role>} authority from the {@code role} claim.
 */
class JwtFilterTest {

    /** 32 base64 bytes — a generated test-only value, not the (now rejected) old dev default. */
    private static final String SECRET = "TCjcvvtjTRHefQlyrDJOratZz+IDf7cGtois83a7iLg=";

    private JwtService jwtService;
    private JwtFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET);
        filter = new JwtFilter(jwtService);
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

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    @Test
    void principalIsTheUuidFromTheSubClaim() throws Exception {
        UUID id = UUID.randomUUID();
        MockHttpServletRequest request = requestWithToken(tokenFor(id, "USER"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(UUID.class);
        assertThat(auth.getPrincipal()).isEqualTo(id);
        verify(chain).doFilter(request, response);
    }

    @Test
    void principalIsNotAnEmailString() throws Exception {
        MockHttpServletRequest request = requestWithToken(tokenFor(UUID.randomUUID(), "USER"));

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isNotInstanceOf(String.class);
    }

    @Test
    void authorityComesFromRoleClaimWithRolePrefix() throws Exception {
        MockHttpServletRequest request = requestWithToken(tokenFor(UUID.randomUUID(), "ADMIN"));

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void invalidTokenLeavesTheContextUnauthenticated() throws Exception {
        MockHttpServletRequest request = requestWithToken("not-a-jwt");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
