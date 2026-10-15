package com.pulsepass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String ISSUER = "pulsepass-api";
    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    // Claves ficticias, solo para tests
    private static final byte[] KEY_BYTES =
            "test-only-key-do-not-use-in-prod".getBytes(StandardCharsets.UTF_8);
    private static final SecretKey KEY = Keys.hmacShaKeyFor(KEY_BYTES);
    private static final SecretKey OTHER_KEY =
            Keys.hmacShaKeyFor("other-test-key-different-32bytes".getBytes(StandardCharsets.UTF_8));

    private final JwtService service = newService(KEY_BYTES, ISSUER, CLOCK);

    // ---------- helpers ----------

    private static JwtService newService(byte[] keyBytes, String issuer, Clock clock) {
        JwtProperties properties = new JwtProperties(
                issuer,
                Base64.getEncoder().encodeToString(keyBytes),
                Duration.ofMinutes(15));
        return new JwtService(properties, clock);
    }

    /** Construye tokens a mano; un parámetro null omite ese claim. key null = sin firma. */
    private static String token(String subject, String issuer, Instant issuedAt,
                                Instant expiration, String jti,
                                SecretKey key, MacAlgorithm algorithm) {
        JwtBuilder builder = Jwts.builder();
        if (subject != null) builder.subject(subject);
        if (issuer != null) builder.issuer(issuer);
        if (issuedAt != null) builder.issuedAt(Date.from(issuedAt));
        if (expiration != null) builder.expiration(Date.from(expiration));
        if (jti != null) builder.id(jti);
        if (key != null) builder.signWith(key, algorithm);
        return builder.compact();
    }

    private static String validToken() {
        return token("42", ISSUER, NOW, NOW.plusSeconds(900),
                UUID.randomUUID().toString(), KEY, Jwts.SIG.HS256);
    }

    private void assertRejected(String token) {
        assertThatThrownBy(() -> service.validateAndExtractUserId(token))
                .isInstanceOf(InvalidJwtException.class);
    }

    // ---------- emisión ----------

    @Test
    void generatesTokenWithExpectedClaimsOnly() {
        String token = service.generateAccessToken(42L);

        Jws<Claims> jws = Jwts.parser()
                .verifyWith(KEY)
                .clock(() -> Date.from(NOW))
                .build()
                .parseSignedClaims(token);
        Claims claims = jws.getPayload();

        assertThat(jws.getHeader().getAlgorithm()).isEqualTo("HS256");
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getIssuedAt().toInstant()).isEqualTo(NOW);
        assertThat(claims.getExpiration().toInstant()).isEqualTo(NOW.plusSeconds(900));
        // Sin datos sensibles en el payload
        assertThat(claims.keySet()).containsExactlyInAnyOrder("sub", "iss", "iat", "exp", "jti");
    }

    @Test
    void exposesTtlInSeconds() {
        assertThat(service.getAccessTokenTtlSeconds()).isEqualTo(900L);
    }

    @Test
    void eachTokenHasDifferentJti() {
        Jws<Claims> first = Jwts.parser().verifyWith(KEY).clock(() -> Date.from(NOW)).build()
                .parseSignedClaims(service.generateAccessToken(42L));
        Jws<Claims> second = Jwts.parser().verifyWith(KEY).clock(() -> Date.from(NOW)).build()
                .parseSignedClaims(service.generateAccessToken(42L));

        assertThat(first.getPayload().getId()).isNotEqualTo(second.getPayload().getId());
    }

    // ---------- validación: caso feliz ----------

    @Test
    void validTokenReturnsUserId() {
        String token = service.generateAccessToken(42L);

        assertThat(service.validateAndExtractUserId(token)).isEqualTo(42L);
    }

    // ---------- validación: rechazos ----------

    @Test
    void expiredTokenIsRejected() {
        Clock oneHourAgo = Clock.fixed(NOW.minus(Duration.ofHours(1)), ZoneOffset.UTC);
        String expired = newService(KEY_BYTES, ISSUER, oneHourAgo).generateAccessToken(42L);

        assertRejected(expired);
    }

    @Test
    void wrongIssuerIsRejected() {
        String foreign = newService(KEY_BYTES, "otro-emisor", CLOCK).generateAccessToken(42L);

        assertRejected(foreign);
    }

    @Test
    void tamperedSignatureIsRejected() {
        String[] parts = service.generateAccessToken(42L).split("\\.");
        char first = parts[2].charAt(0);
        char replacement = (first == 'A') ? 'B' : 'A';

        assertRejected(parts[0] + "." + parts[1] + "." + replacement + parts[2].substring(1));
    }

    @Test
    void tamperedPayloadIsRejected() {
        String[] parts = service.generateAccessToken(42L).split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String forged = payload.replace("\"sub\":\"42\"", "\"sub\":\"1\"");
        assertThat(forged).isNotEqualTo(payload); // la prueba falla si el reemplazo no aplicó

        String forgedPart = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(forged.getBytes(StandardCharsets.UTF_8));

        assertRejected(parts[0] + "." + forgedPart + "." + parts[2]);
    }

    @Test
    void tokenSignedWithOtherKeyIsRejected() {
        String token = token("42", ISSUER, NOW, NOW.plusSeconds(900),
                UUID.randomUUID().toString(), OTHER_KEY, Jwts.SIG.HS256);

        assertRejected(token);
    }

    @Test
    void unsignedTokenIsRejected() {
        String unsigned = token("42", ISSUER, NOW, NOW.plusSeconds(900),
                UUID.randomUUID().toString(), null, null);

        assertRejected(unsigned);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "not-a-jwt", "a.b.c", "eyJhbGciOiJIUzI1NiJ9.e30."})
    void malformedTokensAreRejected(String token) {
        assertRejected(token);
    }

    @Test
    void missingSubjectIsRejected() {
        assertRejected(token(null, ISSUER, NOW, NOW.plusSeconds(900),
                UUID.randomUUID().toString(), KEY, Jwts.SIG.HS256));
    }

    @Test
    void missingExpirationIsRejected() {
        assertRejected(token("42", ISSUER, NOW, null,
                UUID.randomUUID().toString(), KEY, Jwts.SIG.HS256));
    }

    @Test
    void missingJtiIsRejected() {
        assertRejected(token("42", ISSUER, NOW, NOW.plusSeconds(900),
                null, KEY, Jwts.SIG.HS256));
    }

    @Test
    void nonNumericSubjectIsRejected() {
        assertRejected(token("abc", ISSUER, NOW, NOW.plusSeconds(900),
                UUID.randomUUID().toString(), KEY, Jwts.SIG.HS256));
    }

    @Test
    void futureIssuedAtIsRejected() {
        assertRejected(token("42", ISSUER, NOW.plus(Duration.ofMinutes(10)),
                NOW.plus(Duration.ofMinutes(25)), UUID.randomUUID().toString(),
                KEY, Jwts.SIG.HS256));
    }

    @Test
    void tokenWithDifferentAlgorithmIsRejected() {
        // Con una clave de 64 bytes, JJWT aceptaría HS256 y HS512; nosotros solo HS256.
        byte[] longKeyBytes = ("0123456789".repeat(6) + "0123").getBytes(StandardCharsets.UTF_8);
        SecretKey longKey = Keys.hmacShaKeyFor(longKeyBytes);
        JwtService longKeyService = newService(longKeyBytes, ISSUER, CLOCK);

        String hs256 = token("42", ISSUER, NOW, NOW.plusSeconds(900),
                UUID.randomUUID().toString(), longKey, Jwts.SIG.HS256);
        String hs512 = token("42", ISSUER, NOW, NOW.plusSeconds(900),
                UUID.randomUUID().toString(), longKey, Jwts.SIG.HS512);

        assertThat(longKeyService.validateAndExtractUserId(hs256)).isEqualTo(42L);
        assertThatThrownBy(() -> longKeyService.validateAndExtractUserId(hs512))
                .isInstanceOf(InvalidJwtException.class);
    }
}