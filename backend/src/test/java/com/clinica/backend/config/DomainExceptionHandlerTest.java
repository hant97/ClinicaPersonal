package com.clinica.backend.config;

import com.clinica.backend.exception.BusinessRuleException;
import com.clinica.backend.exception.ConflictException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DomainExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsConflictAndBusinessRulesToPredictableResponses() {
        assertEquals(409, handler.handleConflict(new ConflictException("Duplicado")).getStatusCode().value());
        assertEquals(422, handler.handleBusinessRule(new BusinessRuleException("Regla inválida")).getStatusCode().value());
    }
}
