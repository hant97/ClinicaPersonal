package com.clinica.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "risk_assessments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "clinical_session_id", unique = true)
    private Long clinicalSessionId;

    @Column(name = "professional_id")
    private Long professionalId;

    @Column(name = "suicidal_ideation")
    private Boolean suicidalIdeation;

    @Column(name = "ideation_frequency", length = 100)
    private String ideationFrequency;

    @Column(name = "has_plan")
    private Boolean hasPlan;

    @Column(name = "plan_description", columnDefinition = "TEXT")
    private String planDescription;

    @Column(name = "means_access")
    private Boolean meansAccess;

    @Column(name = "means_description", columnDefinition = "TEXT")
    private String meansDescription;

    @Column(name = "protective_factors", columnDefinition = "TEXT")
    private String protectiveFactors;

    @Column(name = "risk_level", length = 50)
    private String riskLevel;

    @Column(name = "action_taken", columnDefinition = "TEXT")
    private String actionTaken;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;
}
