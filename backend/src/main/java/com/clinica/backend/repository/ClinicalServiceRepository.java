package com.clinica.backend.repository;

import com.clinica.backend.model.ClinicalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface ClinicalServiceRepository extends JpaRepository<ClinicalService, Long> {
    Page<ClinicalService> findByDeletedFalse(Pageable pageable);
    Page<ClinicalService> findByNameContainingIgnoreCaseAndDeletedFalse(String name, Pageable pageable);
    Optional<ClinicalService> findByIdAndDeletedFalse(Long id);
    List<ClinicalService> findByDeletedFalse();
}
