package com.clinica.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "inventory_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supply_id", nullable = false)
    private Supply supply;

    @Column(name = "quantity", nullable = false)
    private Integer quantity; // Use positive for IN/RESTOCK, negative for OUT/USAGE/DAMAGED

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_reason", nullable = false)
    private TransactionReason reason;

    @Column(name = "reference_id")
    private String referenceId; // E.g., Payment ID or ClinicalSession ID as string for flexible reference

    @Column(name = "notes")
    private String notes;

    @CreationTimestamp
    @Column(name = "transaction_date", updatable = false, nullable = false)
    private LocalDateTime transactionDate;
}
