package com.clinica.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InitialUserCredentialsTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void seededPasswordsMatchDocumentedTestCredentials() {
        assertTrue(encoder.matches("Admin!1234", "$2a$10$I9dHLFAtRUVzcA8ngzcITeN1q0R2SBfEry0THN8JPmMYwF0b6eAKW"));
        assertTrue(encoder.matches("Psico!1234", "$2a$10$se0lzYQLOIPq7tlmDCKmjeru.B7wY9fiwy38y3ysbByq/EmMPI0zW"));
        assertTrue(encoder.matches("Dermato!1234", "$2a$10$7/MDrYR55wnkGdflK3n9NuFtGhYoONMU.vkRNn4Ox1i96xhtYNC82"));
    }
}
