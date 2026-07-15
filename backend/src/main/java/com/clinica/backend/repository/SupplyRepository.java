package com.clinica.backend.repository;

import com.clinica.backend.model.Supply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplyRepository extends JpaRepository<Supply, Long> {
    List<Supply> findByDeletedFalse();
    Optional<Supply> findByIdAndDeletedFalse(Long id);
    List<Supply> findByDeletedFalseAndCurrentStockLessThanEqual(Integer stockThreshold);
}
