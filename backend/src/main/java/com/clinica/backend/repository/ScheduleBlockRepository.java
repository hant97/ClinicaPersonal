package com.clinica.backend.repository;

import com.clinica.backend.model.ScheduleBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleBlockRepository extends JpaRepository<ScheduleBlock, Long> {

    Optional<ScheduleBlock> findByIdAndSpecialty(Long id, String specialty);

    @Query("SELECT b FROM ScheduleBlock b WHERE b.specialty = :specialty " +
           "AND (:professionalId IS NULL OR b.professionalId IS NULL OR b.professionalId = :professionalId) " +
           "AND b.startDate <= :endDate AND b.endDate >= :startDate " +
           "ORDER BY b.startDate ASC, b.startTime ASC NULLS FIRST")
    List<ScheduleBlock> findBlocksInRange(
            @Param("specialty") String specialty,
            @Param("professionalId") Long professionalId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT b FROM ScheduleBlock b WHERE b.specialty = :specialty " +
           "AND (:professionalId IS NULL OR b.professionalId IS NULL OR b.professionalId = :professionalId) " +
           "AND :date BETWEEN b.startDate AND b.endDate")
    List<ScheduleBlock> findBlocksForDate(
            @Param("specialty") String specialty,
            @Param("professionalId") Long professionalId,
            @Param("date") LocalDate date);
}
