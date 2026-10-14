package com.pulsepass.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderConfigTest {

    private final PasswordEncoder encoder = new PasswordEncoderConfig().passwordEncoder();

    private static final String RAW = "UnaFraseSeguraLarga2026!";

    @Test
    void hashIsNotPlainTextAndMatchesOriginal() {
        String hash = encoder.encode(RAW);

        assertThat(hash).isNotEqualTo(RAW).startsWith("$2");
        assertThat(encoder.matches(RAW, hash)).isTrue();
        assertThat(encoder.matches("otra-clave", hash)).isFalse();
    }

    @Test
    void samePasswordProducesDifferentHashes() {
        assertThat(encoder.encode(RAW)).isNotEqualTo(encoder.encode(RAW));
    }
}