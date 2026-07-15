package com.clinica.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "clinical_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "session_type", nullable = false)
    private String sessionType;

    @Column(name = "modality", nullable = false)
    private String modality;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "subjective", columnDefinition = "TEXT")
    private String subjective;

    @Column(name = "objective", columnDefinition = "TEXT")
    private String objective;

    @Column(name = "analysis", columnDefinition = "TEXT")
    private String analysis;

    @Column(name = "plan", columnDefinition = "TEXT")
    private String plan;

    @Column(name = "is_confidential", nullable = false)
    private boolean isConfidential = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "professional_id")
    private Long professionalId;

    @Column(name = "appointment_id")
    private Long appointmentId;
}
