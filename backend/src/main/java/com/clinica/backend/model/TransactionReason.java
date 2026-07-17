package com.clinica.backend.model;

public enum TransactionReason {
    BILLING,         // Venta / Facturación
    CLINICAL_USAGE,  // Uso en sesión clínica
    EXPIRED,         // Producto caducado
    DAMAGED,         // Producto dañado o merma
    RESTOCK          // Reabastecimiento de inventario
}
