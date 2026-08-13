package com.clinica.backend.repository;

import com.clinica.backend.model.WebsiteBenefit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebsiteBenefitRepository extends JpaRepository<WebsiteBenefit, Long> {
    List<WebsiteBenefit> findAllByOrderByDisplayOrderAscIdAsc();
}
