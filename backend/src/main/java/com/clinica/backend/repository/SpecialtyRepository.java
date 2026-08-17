package com.clinica.backend.repository;

import com.clinica.backend.model.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {

    Optional<Specialty> findByCode(String code);

    boolean existsByCode(String code);

    List<Specialty> findAllByActiveTrueOrderByDisplayOrderAscCodeAsc();

    List<Specialty> findAllByOrderByDisplayOrderAscCodeAsc();
}
