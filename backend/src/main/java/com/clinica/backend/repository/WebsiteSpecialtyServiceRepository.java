package com.clinica.backend.repository;

import com.clinica.backend.model.WebsiteSpecialtyService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebsiteSpecialtyServiceRepository extends JpaRepository<WebsiteSpecialtyService, Long> {
    List<WebsiteSpecialtyService> findAllByOrderByDisplayOrderAscIdAsc();
}
