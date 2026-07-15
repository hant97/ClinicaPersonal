package com.clinica.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "assessments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "psychometric_test_id", nullable = false)
    private PsychometricTest psychometricTest;

    @CreationTimestamp
    @Column(name = "assessment_date", updatable = false)
    private LocalDateTime assessmentDate;

    @Column(name = "total_score")
    private Integer totalScore;

    // JSON representation of the patient's answers
    // Example: {"1": 0, "2": 1, "3": 3} where key is questionId and value is selected option score or ID
    @Column(name = "answers_json", columnDefinition = "TEXT", nullable = false)
    private String answersJson;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
