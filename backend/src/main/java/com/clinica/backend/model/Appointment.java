package com.clinica.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "status", nullable = false)
    private String status; // PROGRAMADA, CONFIRMADA, COMPLETADA, CANCELADA, NO_ASISTIO

    @Column(name = "modality")
    private String modality; // PRESENCIAL, VIRTUAL

    @Column(name = "video_call_link")
    private String videoCallLink;

    @Column(name = "professional_id")
    private Long professionalId;

    @Column(name = "is_first_time", nullable = false)
    private boolean isFirstTime = false;

    @Column(name = "clinical_session_id")
    private Long clinicalSessionId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
