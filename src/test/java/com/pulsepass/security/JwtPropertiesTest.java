package com.pulsepass.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    private static final Duration TTL = Duration.ofMinutes(15);

    private static String base64OfBytes(int length) {
        return Base64.getEncoder().encodeToString(new byte[length]);
    }

    @Test
    void acceptsKeyOf32Bytes() {
        assertThatCode(() -> new JwtProperties("pulsepass-api", base64OfBytes(32), TTL))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsKeyShorterThan32Bytes() {
        assertThatThrownBy(() -> new JwtProperties("pulsepass-api", base64OfBytes(16), TTL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void rejectsInvalidBase64() {
        assertThatThrownBy(() -> new JwtProperties("pulsepass-api", "%%%no-es-base64%%%", TTL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsMissingSecret(String secret) {
        assertThatThrownBy(() -> new JwtProperties("pulsepass-api", secret, TTL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankIssuer() {
        assertThatThrownBy(() -> new JwtProperties(" ", base64OfBytes(32), TTL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPositiveTtl() {
        assertThatThrownBy(() -> new JwtProperties("pulsepass-api", base64OfBytes(32), Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtProperties("pulsepass-api", base64OfBytes(32), Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toStringDoesNotLeakTheSecret() {
        String secret = base64OfBytes(32);

        String text = new JwtProperties("pulsepass-api", secret, TTL).toString();

        assertThat(text).doesNotContain(secret).contains("***");
    }
}