package com.clinica.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "medical_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "session_date", nullable = false)
    private LocalDateTime sessionDate;

    @Column(name = "reason_for_consultation", columnDefinition = "TEXT")
    private String reasonForConsultation;

    @Column(name = "evolution_notes", columnDefinition = "TEXT")
    private String evolutionNotes;

    @Column(name = "presumptive_diagnosis")
    private String presumptiveDiagnosis;

    @Column(name = "agreements_and_tasks", columnDefinition = "TEXT")
    private String agreementsAndTasks;

    // JSONB field for dynamic customizable fields in the future
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dynamic_data", columnDefinition = "jsonb")
    private String dynamicData; 
}
