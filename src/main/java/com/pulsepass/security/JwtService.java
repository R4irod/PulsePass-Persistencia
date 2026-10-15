package com.pulsepass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Emite y valida JWT firmados con HS256 (FR-SECAUTH-005 y 006).
 * Solo conoce tokens: no consulta usuarios, tickets ni reglas de negocio.
 */
@Service
public class JwtService {

    private static final String EXPECTED_ALGORITHM = "HS256";
    private static final Duration IAT_TOLERANCE = Duration.ofSeconds(60);

    private final Clock clock;
    private final String issuer;
    private final Duration accessTokenTtl;
    private final SecretKey key;
    private final JwtParser parser;

    @Autowired
    public JwtService(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    // El Clock inyectable permite probar tokens vencidos sin esperar 15 minutos.
    JwtService(JwtProperties properties, Clock clock) {
        this.clock = clock;
        this.issuer = properties.issuer();
        this.accessTokenTtl = properties.accessTokenTtl();
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secretBase64()));
        this.parser = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    public String generateAccessToken(Long userId) {
        Objects.requireNonNull(userId, "userId es obligatorio");
        Instant now = clock.instant();

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }

    /**
     * Valida el token y devuelve el id de usuario del claim "sub".
     * Lanza InvalidJwtException ante cualquier problema.
     */
    public Long validateAndExtractUserId(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidJwtException("Token vacío");
        }

        Jws<Claims> jws;
        try {
            // Verifica firma, issuer y expiración. Rechaza tokens sin firma (alg=none).
            jws = parser.parseSignedClaims(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidJwtException("Token inválido", e);
        }

        // JJWT acepta HS256/384/512 con la misma clave; aquí solo permitimos HS256.
        if (!EXPECTED_ALGORITHM.equals(jws.getHeader().getAlgorithm())) {
            throw new InvalidJwtException("Algoritmo no permitido");
        }

        Claims claims = jws.getPayload();

        // JJWT no exige que estos claims existan: los exigimos nosotros.
        if (claims.getExpiration() == null) {
            throw new InvalidJwtException("Token sin expiración");
        }
        if (claims.getIssuedAt() == null
                || claims.getIssuedAt().toInstant().isAfter(clock.instant().plus(IAT_TOLERANCE))) {
            throw new InvalidJwtException("Fecha de emisión inválida");
        }
        if (claims.getId() == null || claims.getId().isBlank()) {
            throw new InvalidJwtException("Token sin identificador");
        }

        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new InvalidJwtException("Token sin sujeto");
        }
        try {
            long userId = Long.parseLong(subject);
            if (userId <= 0) {
                throw new InvalidJwtException("Sujeto inválido");
            }
            return userId;
        } catch (NumberFormatException e) {
            throw new InvalidJwtException("Sujeto inválido", e);
        }
    }
}