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
@Table(name = "lesions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Patient patient;

    @Column(name = "body_area", length = 100)
    private String bodyArea;

    @Column(name = "lesion_type", length = 100)
    private String lesionType;

    @Column(name = "size", length = 50)
    private String size;

    @Column(name = "morphology", length = 255)
    private String morphology;

    @Column(name = "color", length = 100)
    private String color;

    @Column(name = "since_date")
    private LocalDate sinceDate;

    @Column(name = "evolution", columnDefinition = "TEXT")
    private String evolution;

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
