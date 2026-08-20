package com.clinica.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"items", "transactions"})
@ToString(exclude = {"items", "transactions"})
public class Payment {

    public static final String STATUS_PENDIENTE = "PENDIENTE";
    public static final String STATUS_PARCIAL = "PARCIAL";
    public static final String STATUS_PAGADO = "PAGADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "payment_method")
    private String paymentMethod; // Código de catálogo PAYMENT_METHOD (EFECTIVO, TRANSFERENCIA, ...)

    @Column(name = "status", nullable = false, length = 20)
    private String status = STATUS_PENDIENTE;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_session_id")
    private ClinicalSession clinicalSession;

    @Column(name = "attention_id")
    private Long attentionId;

    @Column(name = "description")
    private String description;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<PaymentItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<PaymentTransaction> transactions = new ArrayList<>();

    @Column(name = "specialty", nullable = false)
    private String specialty = "PSICOLOGIA";
}
