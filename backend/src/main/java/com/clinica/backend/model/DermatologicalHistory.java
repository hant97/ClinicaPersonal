package com.clinica.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "dermatological_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DermatologicalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Patient patient;

    @Column(name = "skin_type", length = 50)
    private String skinType;

    @Column(name = "sun_exposure_habits", columnDefinition = "TEXT")
    private String sunExposureHabits;

    @Column(name = "personal_skin_history", columnDefinition = "TEXT")
    private String personalSkinHistory;

    @Column(name = "family_skin_history", columnDefinition = "TEXT")
    private String familySkinHistory;

    @Column(name = "chronic_conditions", columnDefinition = "TEXT")
    private String chronicConditions;

    @Column(name = "exam_findings", columnDefinition = "TEXT")
    private String examFindings;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

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
