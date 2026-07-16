package com.clinica.backend.repository;

import com.clinica.backend.model.Supply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplyRepository extends JpaRepository<Supply, Long> {
    Page<Supply> findByDeletedFalse(Pageable pageable);
    Page<Supply> findByNameContainingIgnoreCaseAndDeletedFalse(String name, Pageable pageable);
    List<Supply> findByDeletedFalse(); // Keep original if needed elsewhere
    Optional<Supply> findByIdAndDeletedFalse(Long id);
    List<Supply> findByDeletedFalseAndCurrentStockLessThanEqual(Integer stockThreshold);
}
