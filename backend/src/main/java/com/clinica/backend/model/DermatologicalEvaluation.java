package com.clinica.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "dermatological_evaluations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DermatologicalEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Patient patient;

    @Column(name = "evaluation_date", nullable = false)
    private LocalDate evaluationDate;

    @Column(name = "skin_type", length = 100)
    private String skinType;

    @Column(name = "affected_area", length = 100)
    private String affectedArea;

    @Column(name = "lesion_type", length = 100)
    private String lesionType;

    @Column(name = "lesion_size", length = 50)
    private String lesionSize;

    @Column(name = "dermatological_diagnosis", columnDefinition = "TEXT")
    private String dermatologicalDiagnosis;

    @Column(name = "treatment_indicated", columnDefinition = "TEXT")
    private String treatmentIndicated;

    @Column(name = "procedure_performed", columnDefinition = "TEXT")
    private String procedurePerformed;

    @Column(name = "evolution_notes", columnDefinition = "TEXT")
    private String evolutionNotes;

    @Column(name = "next_review_date")
    private LocalDate nextReviewDate;

    @Column(name = "professional_id")
    private Long professionalId;

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
