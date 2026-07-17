package com.clinica.backend.model;

public enum TransactionType {
    IN,         // Entrada de inventario
    OUT,        // Salida de inventario
    ADJUSTMENT  // Ajuste general (puede ser positivo o negativo)
}
