package com.clinica.backend.config;

import com.clinica.backend.dto.ApiErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerSecurityTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void shouldSanitizeDataIntegrityViolationExceptionWithoutExposingSqlDetails() {
        String sensitiveSqlError = "ERROR: duplicate key value violates unique constraint \"users_username_key\" Key (username)=(admin) already exists on table users";
        DataIntegrityViolationException ex = new DataIntegrityViolationException(sensitiveSqlError);

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("No se pudo procesar la solicitud debido a un conflicto o restricción de datos", response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().contains("users_username_key"), "No debe exponer nombres de constraints de BD");
        assertFalse(response.getBody().getMessage().contains("already exists on table"), "No debe exponer tablas SQL");
    }

    @Test
    void shouldHandleMaxUploadSizeExceededException() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(5000000);
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleMaxUploadSizeExceeded(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("El tamaño del archivo excede el límite máximo permitido", response.getBody().getMessage());
    }

    @Test
    void shouldHandleHttpRequestMethodNotSupportedException() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleMethodNotSupported(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Método HTTP no soportado para este endpoint", response.getBody().getMessage());
    }
}
