package com.clinica.backend.repository;

import com.clinica.backend.model.WebsiteSpecialty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebsiteSpecialtyRepository extends JpaRepository<WebsiteSpecialty, Long> {
    List<WebsiteSpecialty> findAllByOrderByDisplayOrderAscIdAsc();
}
