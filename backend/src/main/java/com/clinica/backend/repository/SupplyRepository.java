package com.clinica.backend.repository;

import com.clinica.backend.model.Supply;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplyRepository extends JpaRepository<Supply, Long> {
    Page<Supply> findBySpecialtyAndDeletedFalse(String specialty, Pageable pageable);
    Page<Supply> findBySpecialtyAndNameContainingIgnoreCaseAndDeletedFalse(String specialty, String name, Pageable pageable);
    List<Supply> findBySpecialtyAndDeletedFalse(String specialty);
    Optional<Supply> findByIdAndDeletedFalse(Long id);
    Optional<Supply> findByIdAndSpecialtyAndDeletedFalse(Long id, String specialty);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Supply s WHERE s.id = :id AND s.deleted = false")
    Optional<Supply> findActiveByIdForUpdate(@Param("id") Long id);

    @Query("SELECT s FROM Supply s WHERE s.deleted = false AND s.specialty = :specialty AND s.currentStock IS NOT NULL AND s.minStockLevel IS NOT NULL AND s.currentStock <= s.minStockLevel")
    List<Supply> findLowStockSuppliesBySpecialty(String specialty);

    long countBySpecialtyAndDeletedFalse(String specialty);

    @Query("SELECT COALESCE(SUM(COALESCE(s.price, 0) * s.currentStock), 0) FROM Supply s WHERE s.deleted = false AND s.specialty = :specialty")
    BigDecimal sumInventoryValueBySpecialty(@Param("specialty") String specialty);

    @Query("SELECT COUNT(s) FROM Supply s WHERE s.deleted = false AND s.specialty = :specialty AND s.currentStock = 0")
    long countOutOfStockBySpecialty(@Param("specialty") String specialty);

    @Query("SELECT COUNT(s) FROM Supply s WHERE s.deleted = false AND s.specialty = :specialty AND s.currentStock > 0 AND s.currentStock <= s.minStockLevel")
    long countLowStockBySpecialty(@Param("specialty") String specialty);

    @Query("SELECT COUNT(s) FROM Supply s WHERE s.deleted = false AND s.specialty = :specialty AND s.expirationDate IS NOT NULL AND s.expirationDate BETWEEN :startDate AND :endDate")
    long countExpiringBetweenBySpecialty(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("specialty") String specialty);
}
