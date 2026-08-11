package com.clinica.backend.repository;

import com.clinica.backend.model.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    Page<Appointment> findByPatientIdOrderByAppointmentDateDescStartTimeDesc(Long patientId, Pageable pageable);
    Page<Appointment> findAllByOrderByAppointmentDateAscStartTimeAsc(Pageable pageable);
    List<Appointment> findByAppointmentDateAndStatusNot(LocalDate date, String status);

    long countByAppointmentDate(LocalDate date);

    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate >= :startDate AND a.appointmentDate <= :endDate")
    List<Appointment> findByAppointmentDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient p " +
           "WHERE (a.appointmentDate > :today OR (a.appointmentDate = :today AND (a.startTime IS NULL OR a.startTime >= :nowTime))) " +
           "AND a.status NOT IN ('CANCELADA', 'NO_ASISTIO', 'COMPLETADA') " +
           "ORDER BY a.appointmentDate ASC, a.startTime ASC")
    List<Appointment> findUpcomingAppointments(@Param("today") LocalDate today, @Param("nowTime") LocalTime nowTime, Pageable pageable);

    @Query("SELECT a FROM Appointment a " +
           "WHERE (LOWER(a.patient.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(a.patient.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (cast(:startDate as date) IS NULL OR a.appointmentDate >= :startDate) " +
           "AND (cast(:endDate as date) IS NULL OR a.appointmentDate <= :endDate) " +
           "ORDER BY a.appointmentDate ASC, a.startTime ASC")
    Page<Appointment> searchAppointments(
            @Param("searchTerm") String searchTerm,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);
}
