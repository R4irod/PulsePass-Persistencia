package com.pulsepass.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Base64;

@ConfigurationProperties(prefix = "pulsepass.security.jwt")
public record JwtProperties(String issuer, String secretBase64, Duration accessTokenTtl) {

    private static final int MIN_KEY_BYTES = 32;

    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
        throw new IllegalArgumentException("pulsepass.security.jwt.issuer es obligatorio");
        }
        
        if (secretBase64 == null || secretBase64.isBlank() || secretBase64.startsWith("${")) {
            throw new IllegalArgumentException(
                    "Falta la clave JWT: defina PULSEPASS_JWT_SECRET_BASE64");
        }
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalArgumentException(
                    "Falta la clave JWT: defina PULSEPASS_JWT_SECRET_BASE64");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(secretBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La clave JWT no es Base64 válido");
        }
        if (decoded.length < MIN_KEY_BYTES) {
            throw new IllegalArgumentException(
                    "La clave JWT debe tener al menos " + MIN_KEY_BYTES + " bytes");
        }
        if (accessTokenTtl == null || accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
            throw new IllegalArgumentException("access-token-ttl debe ser una duración positiva");
        }
    }

    // Evita que el secreto aparezca en logs (NFR-SEC001)
    @Override
    public String toString() {
        return "JwtProperties[issuer=" + issuer + ", secretBase64=***, accessTokenTtl=" + accessTokenTtl + "]";
    }
}