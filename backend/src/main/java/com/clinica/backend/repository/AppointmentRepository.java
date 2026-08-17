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
    Page<Appointment> findByPatientIdAndSpecialtyOrderByAppointmentDateDescStartTimeDesc(Long patientId, String specialty, Pageable pageable);
    Page<Appointment> findAllBySpecialtyOrderByAppointmentDateAscStartTimeAsc(String specialty, Pageable pageable);
    List<Appointment> findByAppointmentDateAndStatusNotAndSpecialty(LocalDate date, String status, String specialty);

    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate = :date " +
           "AND a.status NOT IN ('CANCELADA') " +
           "AND a.specialty = :specialty " +
           "AND (:professionalId IS NULL OR a.professionalId IS NULL OR a.professionalId = :professionalId) " +
           "AND (a.startTime < :endTime AND a.endTime > :startTime)")
    List<Appointment> findOverlappingAppointments(
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("specialty") String specialty,
            @Param("professionalId") Long professionalId);

    long countByAppointmentDateAndSpecialty(LocalDate date, String specialty);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient p " +
           "WHERE a.appointmentDate = :date AND a.specialty = :specialty " +
           "ORDER BY a.startTime ASC NULLS LAST")
    List<Appointment> findTodayAppointmentsBySpecialty(@Param("date") LocalDate date, @Param("specialty") String specialty);

    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate >= :startDate AND a.appointmentDate <= :endDate AND a.specialty = :specialty")
    List<Appointment> findByAppointmentDateBetweenAndSpecialty(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("specialty") String specialty);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient p " +
           "WHERE (a.appointmentDate > :today OR (a.appointmentDate = :today AND (a.startTime IS NULL OR a.startTime >= :nowTime))) " +
           "AND a.status NOT IN ('CANCELADA', 'NO_ASISTIO', 'COMPLETADA') " +
           "AND a.specialty = :specialty " +
           "ORDER BY a.appointmentDate ASC, a.startTime ASC")
    List<Appointment> findUpcomingAppointmentsBySpecialty(@Param("today") LocalDate today, @Param("nowTime") LocalTime nowTime, @Param("specialty") String specialty, Pageable pageable);

    @Query("SELECT a FROM Appointment a " +
           "WHERE (LOWER(a.patient.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(a.patient.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (cast(:startDate as date) IS NULL OR a.appointmentDate >= :startDate) " +
           "AND (cast(:endDate as date) IS NULL OR a.appointmentDate <= :endDate) " +
           "AND a.specialty = :specialty " +
           "ORDER BY a.appointmentDate ASC, a.startTime ASC")
    Page<Appointment> searchAppointmentsBySpecialty(
            @Param("searchTerm") String searchTerm,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("specialty") String specialty,
            Pageable pageable);
}
