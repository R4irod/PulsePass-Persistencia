package com.pulsepass.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private AuthenticationEntryPoint entryPoint;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService, entryPoint);
        request = new MockHttpServletRequest("GET", "/api/auth/me");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private SecurityPrincipal principal(boolean enabled) {
        return new SecurityPrincipal(7L, "andrea", "andrea@test.com", null, enabled,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private void assertRejected() throws Exception {
        verify(entryPoint).commence(any(HttpServletRequest.class), any(HttpServletResponse.class),
                any(AuthenticationException.class));
        assertThat(chain.getRequest()).as("la cadena no debe continuar").isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void withoutAuthorizationHeaderChainContinuesUnauthenticated() throws Exception {
        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService, userDetailsService, entryPoint);
    }

    @Test
    void nonBearerSchemeIsIgnored() throws Exception {
        request.addHeader("Authorization", "Basic YWxndW5vOmNsYXZl");

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService, userDetailsService, entryPoint);
    }

    @Test
    void validTokenBuildsAuthenticationFromCurrentDatabasePrincipal() throws Exception {
        request.addHeader("Authorization", "Bearer token-valido");
        when(jwtService.validateAndExtractUserId("token-valido")).thenReturn(7L);
        when(userDetailsService.loadUserById(7L)).thenReturn(principal(true));

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(SecurityPrincipal.class);
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(entryPoint);
    }

    @Test
    void invalidTokenIsRejected() throws Exception {
        request.addHeader("Authorization", "Bearer token-malo");
        when(jwtService.validateAndExtractUserId("token-malo"))
                .thenThrow(new InvalidJwtException("Token inválido"));

        filter.doFilter(request, response, chain);

        assertRejected();
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void emptyBearerTokenIsRejected() throws Exception {
        request.addHeader("Authorization", "Bearer ");
        when(jwtService.validateAndExtractUserId("")).thenThrow(new InvalidJwtException("Token vacío"));

        filter.doFilter(request, response, chain);

        assertRejected();
    }

    @Test
    void tokenOfDeletedUserIsRejected() throws Exception {
        request.addHeader("Authorization", "Bearer token-valido");
        when(jwtService.validateAndExtractUserId("token-valido")).thenReturn(7L);
        when(userDetailsService.loadUserById(7L)).thenThrow(new UsernameNotFoundException("x"));

        filter.doFilter(request, response, chain);

        assertRejected();
    }

    @Test
    void tokenOfInactiveUserIsRejected() throws Exception {
        request.addHeader("Authorization", "Bearer token-valido");
        when(jwtService.validateAndExtractUserId("token-valido")).thenReturn(7L);
        when(userDetailsService.loadUserById(7L)).thenReturn(principal(false));

        filter.doFilter(request, response, chain);

        assertRejected();
    }

    @Test
    void multipleAuthorizationHeadersAreRejectedWithoutParsingAny() throws Exception {
        request.addHeader("Authorization", "Bearer uno");
        request.addHeader("Authorization", "Bearer dos");

        filter.doFilter(request, response, chain);

        assertRejected();
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void existingAuthenticationIsNeverOverwritten() throws Exception {
        Authentication previous = UsernamePasswordAuthenticationToken.authenticated("previo", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(previous);
        request.addHeader("Authorization", "Bearer otro-token");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(previous);
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(jwtService, userDetailsService, entryPoint);
    }
}