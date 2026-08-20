package com.clinica.backend.repository;

import com.clinica.backend.model.ProfessionalSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfessionalScheduleRepository extends JpaRepository<ProfessionalSchedule, Long> {
    List<ProfessionalSchedule> findByProfessionalIdAndSpecialtyOrderByDayOfWeekAscStartTimeAsc(Long professionalId, String specialty);

    List<ProfessionalSchedule> findByProfessionalIdAndSpecialtyAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(Long professionalId, String specialty);

    List<ProfessionalSchedule> findByProfessionalIdAndSpecialtyAndDayOfWeekAndIsActiveTrue(Long professionalId, String specialty, Integer dayOfWeek);

    boolean existsByProfessionalIdAndSpecialtyAndIsActiveTrue(Long professionalId, String specialty);

    void deleteByProfessionalIdAndSpecialty(Long professionalId, String specialty);
}
