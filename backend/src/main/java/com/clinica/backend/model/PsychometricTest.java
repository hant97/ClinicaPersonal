package com.clinica.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "psychometric_tests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PsychometricTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // JSON representation of the test questions and structure
    // Example: [{"id": 1, "text": "Pregunta 1", "options": [{"score": 0, "text": "Nunca"}, {"score": 1, "text": "A veces"}]}]
    @Column(name = "questions_json", columnDefinition = "TEXT", nullable = false)
    private String questionsJson;
}
