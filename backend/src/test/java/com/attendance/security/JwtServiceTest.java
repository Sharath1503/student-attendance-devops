package com.attendance.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.attendance.domain.Role;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-signing-key-unit-test-signing-key-12345";

    @Test
    void validTokenReturnsUsername() {
        JwtService jwt = new JwtService(SECRET, 5);
        String token = jwt.generateToken("alice", Role.FACULTY);
        assertEquals("alice", jwt.validateAndGetUsername(token));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, -1); // already expired when issued
        String token = jwt.generateToken("alice", Role.STUDENT);
        assertThrows(JwtException.class, () -> jwt.validateAndGetUsername(token));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String forged = new JwtService("another-signing-key-another-signing-key-9876", 5)
                .generateToken("alice", Role.ADMIN);
        JwtService jwt = new JwtService(SECRET, 5);
        assertThrows(JwtException.class, () -> jwt.validateAndGetUsername(forged));
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, 5);
        String token = jwt.generateToken("alice", Role.STUDENT);
        String tampered = token.substring(0, token.length() - 3) + "abc";
        assertThrows(JwtException.class, () -> jwt.validateAndGetUsername(tampered));
    }

    @Test
    void garbageIsRejected() {
        JwtService jwt = new JwtService(SECRET, 5);
        assertThrows(JwtException.class, () -> jwt.validateAndGetUsername("not-a-jwt"));
    }

    @Test
    void shortOrMissingSecretIsRefusedAtStartup() {
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 5));
        assertThrows(IllegalStateException.class, () -> new JwtService(null, 5));
    }
}
