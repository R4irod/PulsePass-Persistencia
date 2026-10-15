package com.pulsepass.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Lee "Authorization: Bearer <jwt>", valida el token y construye el contexto de seguridad
 * con el usuario y los roles ACTUALES de la base de datos.
 * NO es un @Component: se crea en SecurityConfig para que corra una sola vez.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   CustomUserDetailsService userDetailsService,
                                   AuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // Nunca pisar una autenticación ya establecida.
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        List<String> headers = Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION));

        // Sin credenciales: la cadena decide si la ruta es pública o responde 401.
        if (headers.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        // Varios encabezados Authorization: sospechoso.
        if (headers.size() > 1) {
            reject(request, response, "Multiple Authorization headers");
            return;
        }

        String header = headers.get(0);

        // Otro esquema (p. ej. Basic): no es un Bearer, no se autentica.
        if (!header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();

        SecurityPrincipal principal;
        try {
            Long userId = jwtService.validateAndExtractUserId(token);
            principal = userDetailsService.loadUserById(userId); // estado y roles vigentes (SEC011)
        } catch (InvalidJwtException e) {
            reject(request, response, e.getMessage());
            return;
        } catch (UsernameNotFoundException e) {
            reject(request, response, "Token subject not found");
            return;
        }

        if (!principal.isEnabled()) {
            reject(request, response, "User is inactive");
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        chain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request,
                        HttpServletResponse response,
                        String reason) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        // Nunca se registra el token, solo el motivo genérico.
        log.warn("Bearer token rejected for {} {}: {}", request.getMethod(), request.getRequestURI(), reason);
        authenticationEntryPoint.commence(request, response, new BadCredentialsException("Invalid bearer token"));
    }
}