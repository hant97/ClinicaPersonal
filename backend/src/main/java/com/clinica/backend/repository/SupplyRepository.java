package com.clinica.backend.repository;

import com.clinica.backend.model.Supply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplyRepository extends JpaRepository<Supply, Long> {
    Page<Supply> findBySpecialtyAndDeletedFalse(String specialty, Pageable pageable);
    Page<Supply> findBySpecialtyAndNameContainingIgnoreCaseAndDeletedFalse(String specialty, String name, Pageable pageable);
    List<Supply> findBySpecialtyAndDeletedFalse(String specialty);
    Optional<Supply> findByIdAndDeletedFalse(Long id);
    Optional<Supply> findByIdAndSpecialtyAndDeletedFalse(Long id, String specialty);
    
    @Query("SELECT s FROM Supply s WHERE s.deleted = false AND s.specialty = :specialty AND s.currentStock IS NOT NULL AND s.minStockLevel IS NOT NULL AND s.currentStock <= s.minStockLevel")
    List<Supply> findLowStockSuppliesBySpecialty(String specialty);
}
