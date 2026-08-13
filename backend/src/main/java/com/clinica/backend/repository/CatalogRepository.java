package com.clinica.backend.repository;

import com.clinica.backend.model.Catalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Repository
public interface CatalogRepository extends JpaRepository<Catalog, Long> {
    Optional<Catalog> findByCode(String code);
    Page<Catalog> findBySpecialty(String specialty, Pageable pageable);
}
