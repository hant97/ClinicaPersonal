package com.clinica.backend.repository;

import com.clinica.backend.model.Appointment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface AppointmentScheduleLockRepository extends Repository<Appointment, Long> {

    @Query(value = "SELECT pg_advisory_xact_lock(" +
            "hashtext(CAST(:specialty AS text)), " +
            "CAST(CAST(:appointmentDate AS date) - DATE '2000-01-01' AS integer))",
            nativeQuery = true)
    Object acquireScheduleLock(
            @Param("specialty") String specialty,
            @Param("appointmentDate") LocalDate appointmentDate);
}
