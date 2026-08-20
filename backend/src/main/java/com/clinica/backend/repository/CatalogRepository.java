package com.clinica.backend.repository;

import com.clinica.backend.model.Catalog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatalogRepository extends JpaRepository<Catalog, Long> {
    Optional<Catalog> findByCode(String code);
    boolean existsByCode(String code);
    Page<Catalog> findBySpecialty(String specialty, Pageable pageable);

    @Query("SELECT c FROM Catalog c WHERE " +
           "(cast(:specialty as string) IS NULL OR cast(:specialty as string) = 'ALL' OR c.specialty = 'GENERAL' OR c.specialty = :specialty) AND " +
           "(cast(:search as string) IS NULL OR cast(:search as string) = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', cast(:search as string), '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', cast(:search as string), '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', cast(:search as string), '%'))) " +
           "ORDER BY c.name ASC")
    Page<Catalog> findAccessibleCatalogs(@Param("specialty") String specialty, @Param("search") String search, Pageable pageable);

    @Query("SELECT c FROM Catalog c WHERE " +
           "(:specialty IS NULL OR :specialty = 'ALL' OR c.specialty = 'GENERAL' OR c.specialty = :specialty) " +
           "ORDER BY c.name ASC")
    List<Catalog> findAllAccessible(@Param("specialty") String specialty);
}
