package com.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    @Test
    void constructorRejectsBlankInvalidAndShortSecrets() {
        assertThrows(IllegalStateException.class, () -> new JwtService("   ", 86400000L));
        assertThrows(IllegalStateException.class, () -> new JwtService("not!valid!base64!", 86400000L));

        String shortSecret = Base64.getEncoder().encodeToString("too-short-key".getBytes());
        assertThrows(IllegalStateException.class, () -> new JwtService(shortSecret, 86400000L));

        String validSecret = Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes());
        assertDoesNotThrow(() -> new JwtService(validSecret, 86400000L));
    }
}
