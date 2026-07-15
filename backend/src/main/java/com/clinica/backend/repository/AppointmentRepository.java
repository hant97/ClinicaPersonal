package com.clinica.backend.repository;
import com.clinica.backend.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientIdOrderByAppointmentDateDesc(Long patientId);
    List<Appointment> findAllByOrderByAppointmentDateAsc();
    List<Appointment> findByAppointmentDateBetweenAndStatusNot(LocalDateTime start, LocalDateTime end, String status);
}
