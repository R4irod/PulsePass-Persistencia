package com.pulsepass.security;

/**
 * Cualquier token inválido (vencido, manipulado, issuer erróneo, malformado...).
 * El mensaje nunca incluye el token, para no filtrarlo a logs.
 */
public class InvalidJwtException extends RuntimeException {

    public InvalidJwtException(String message) {
        super(message);
    }

    public InvalidJwtException(String message, Throwable cause) {
        super(message, cause);
    }
}