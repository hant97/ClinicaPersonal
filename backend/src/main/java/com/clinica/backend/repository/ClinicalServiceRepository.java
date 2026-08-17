package com.clinica.backend.repository;

import com.clinica.backend.model.ClinicalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

@Repository
public interface ClinicalServiceRepository extends JpaRepository<ClinicalService, Long> {
    Page<ClinicalService> findBySpecialtyAndDeletedFalse(String specialty, Pageable pageable);
    Page<ClinicalService> findBySpecialtyAndNameContainingIgnoreCaseAndDeletedFalse(String specialty, String name, Pageable pageable);
    Optional<ClinicalService> findByIdAndDeletedFalse(Long id);
    Optional<ClinicalService> findByIdAndSpecialtyAndDeletedFalse(Long id, String specialty);
    List<ClinicalService> findBySpecialtyAndDeletedFalse(String specialty);
    List<ClinicalService> findBySpecialtyAndActiveTrueAndDeletedFalse(String specialty);
    List<ClinicalService> findAllByDeletedFalseOrderBySpecialtyAscNameAsc();

    long countBySpecialtyAndDeletedFalse(String specialty);

    @Query("SELECT c FROM ClinicalService c WHERE c.deleted = false AND c.specialty = :specialty AND " +
           "(:name IS NULL OR :name = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:category IS NULL OR :category = '' OR c.category = :category) AND " +
           "(:active IS NULL OR c.active = :active) AND " +
           "(:minPrice IS NULL OR c.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR c.price <= :maxPrice) " +
           "ORDER BY c.name ASC")
    Page<ClinicalService> findAllWithFilters(@Param("name") String name,
                                             @Param("category") String category,
                                             @Param("active") Boolean active,
                                             @Param("minPrice") BigDecimal minPrice,
                                             @Param("maxPrice") BigDecimal maxPrice,
                                             @Param("specialty") String specialty,
                                             Pageable pageable);

    @Query("SELECT COUNT(c) FROM ClinicalService c WHERE c.deleted = false AND c.specialty = :specialty AND c.active = true")
    long countActiveBySpecialty(@Param("specialty") String specialty);

    @Query("SELECT COALESCE(AVG(c.price), 0) FROM ClinicalService c WHERE c.deleted = false AND c.specialty = :specialty AND c.active = true")
    BigDecimal averagePriceBySpecialty(@Param("specialty") String specialty);
}
