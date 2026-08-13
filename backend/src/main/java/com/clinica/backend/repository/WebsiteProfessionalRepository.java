package com.clinica.backend.repository;

import com.clinica.backend.model.WebsiteProfessional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebsiteProfessionalRepository extends JpaRepository<WebsiteProfessional, Long> {
    List<WebsiteProfessional> findAllByOrderByDisplayOrderAscIdAsc();
}
