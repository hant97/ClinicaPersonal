package com.clinica.backend.repository;

import com.clinica.backend.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientIdOrderByAppointmentDateDescStartTimeDesc(Long patientId);
    List<Appointment> findAllByOrderByAppointmentDateAscStartTimeAsc();
    List<Appointment> findByAppointmentDateAndStatusNot(LocalDate date, String status);

    @Query("SELECT a FROM Appointment a " +
           "WHERE (LOWER(a.patient.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(a.patient.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (cast(:startDate as date) IS NULL OR a.appointmentDate >= :startDate) " +
           "AND (cast(:endDate as date) IS NULL OR a.appointmentDate <= :endDate) " +
           "ORDER BY a.appointmentDate ASC, a.startTime ASC")
    List<Appointment> searchAppointments(
            @Param("searchTerm") String searchTerm,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
