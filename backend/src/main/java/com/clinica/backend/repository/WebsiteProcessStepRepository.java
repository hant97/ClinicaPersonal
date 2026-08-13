package com.clinica.backend.repository;

import com.clinica.backend.model.WebsiteProcessStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebsiteProcessStepRepository extends JpaRepository<WebsiteProcessStep, Long> {
    List<WebsiteProcessStep> findAllByOrderByDisplayOrderAscIdAsc();
}
